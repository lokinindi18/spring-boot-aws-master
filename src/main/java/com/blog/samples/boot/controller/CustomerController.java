package com.blog.samples.boot.controller;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.blog.samples.boot.exception.CustomerNotFoundException;
import com.blog.samples.boot.exception.InvalidCustomerRequestException;
import com.blog.samples.boot.model.Address;
import com.blog.samples.boot.model.Customer;
import com.blog.samples.boot.model.CustomerImage;
import com.blog.samples.boot.repository.CustomerRepository;
import com.blog.samples.boot.service.FileArchiveService;

/**
 * Customer Controller exposes a series of RESTful endpoints
 */
@RestController
public class CustomerController {

	private static final Logger log = Logger.getLogger(CustomerController.class);

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private FileArchiveService fileArchiveService;

	@RequestMapping(value = "/customers", method = RequestMethod.POST)
	public @ResponseBody Customer createCustomer(@RequestParam(value = "firstName", required = true) String firstName,
			@RequestParam(value = "lastName", required = true) String lastName,
			@RequestParam(value = "dateOfBirth", required = true) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateOfBirth,
			@RequestParam(value = "street", required = true) String street,
			@RequestParam(value = "town", required = true) String town,
			@RequestParam(value = "county", required = true) String county,
			@RequestParam(value = "postcode", required = true) String postcode,
			@RequestParam(value = "image", required = true) MultipartFile image) {

		CustomerImage customerImage = fileArchiveService.saveFileToS3(image);
		Customer customer = new Customer(firstName, lastName, dateOfBirth, customerImage,
				new Address(street, town, county, postcode));

		customerRepository.save(customer);
		return customer;
	}

	/**
	 * Get customer using id. Returns HTTP 404 if customer not found
	 * 
	 * @param customerId
	 * @return retrieved customer
	 */
	@RequestMapping(value = "/customers/{customerId}", method = RequestMethod.GET)
	public Customer getCustomer(@PathVariable("customerId") Long customerId) {

		/* validate customer Id parameter */
		if (null == customerId) {
			throw new InvalidCustomerRequestException();
		}

		Customer customer = customerRepository.findOne(customerId);

		if (null == customer) {
			throw new CustomerNotFoundException();
		}

		return customer;
	}

	@RequestMapping(value = "/test", method = RequestMethod.GET)
	@ResponseBody
	@Cacheable("calculateResult")
	public String calculateResult() {
		log.debug("Performing expensive calculation...");
		// perform computationally expensive calculation
		return "result";
	}

	@Autowired
	private RedisTemplate redisTemplate;

	/**
	 * Gets all customers.
	 *
	 * @return the customers
	 */
	/*
	 * @RequestMapping(value = "/customers", method = RequestMethod.GET) MAKE SURE
	 * CUSTOMER DTO IS SERIALIZABLE OTHERWISE WILL FAIL Simple way to cache results.
	 * until the timeout is over doesn't matter however time you call this method,
	 * it will return data from the cache
	 * 
	 * @Cacheable("MY_CUSTOMER_LIST") public List<Customer> getCustomers() {
	 * System.out.println("Controller:-> Get Customer invoked..."); return
	 * (List<Customer>) customerRepository.findAll(); }
	 */

	/**
	 * Now try out taking explicit handle of RedisTemplate and put key, set expiry
	 * etc
	 */
	@RequestMapping(value = "/customers", method = RequestMethod.GET)
	public List<Customer> getCustomers() {
		List<Customer> list = null;
		if (redisTemplate.opsForValue().get("MY_CUSTOMER_LIST") == null) {
			System.out.println("Controller:-> calling get customer repository ...");
			list = (List<Customer>) customerRepository.findAll();
			redisTemplate.opsForValue().set("MY_CUSTOMER_LIST", list);
		}
		list = (List<Customer>) redisTemplate.opsForValue().get("MY_CUSTOMER_LIST");
		return list;
	}

	/**
	 * Deletes the customer with given customer id if it exists and returns HTTP204.
	 *
	 * @param customerId
	 *            the customer id
	 */
	@RequestMapping(value = "/customers/{customerId}", method = RequestMethod.DELETE)
	public void removeCustomer(@PathVariable("customerId") Long customerId, HttpServletResponse httpResponse) {

		if (customerRepository.exists(customerId)) {
			Customer customer = customerRepository.findOne(customerId);
			fileArchiveService.deleteImageFromS3(customer.getCustomerImage());
			customerRepository.delete(customer);
			// update the cache & remove the deleted customer
			List<Customer> list = (List<Customer>) redisTemplate.opsForValue().get("MY_CUSTOMER_LIST");
			if (list != null) {
				for (Customer cust : list) {
					if (cust.getId() == customerId) {
						System.out.println("Deleteing customer with Id: " + customerId);
						list.remove(cust);
						redisTemplate.opsForValue().set("MY_CUSTOMER_LIST", list);
						break;
					}
				}
			}
		}

		httpResponse.setStatus(HttpStatus.NO_CONTENT.value());
	}

}