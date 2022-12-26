package lombok.controller;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.LombokDemoNApplication;
import lombok.model.Customer;
import lombok.model.Group;

@Controller
public class LombokController implements CommandLineRunner {

	@Autowired
	JdbcTemplate jdbcTemplate;

	private static final Logger log = LoggerFactory.getLogger(LombokDemoNApplication.class);

	@GetMapping("/")
	public String getIndex() {
		try {
			run();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "index";
	}

	@GetMapping("/select")
	public String getSelect(Model model) {
		
		model.addAttribute("customer", getCustomer("Josh"));
		
		model.addAttribute("group", getGroup("0001"));
		
		return "select";
	}

	public void run(String... strings) throws Exception {

		// ログ info
		log.info("Creating tables");

		// 顧客テーブル削除実行
		jdbcTemplate.execute("DROP TABLE customers IF EXISTS");
		// 顧客テーブル作成実行
		jdbcTemplate.execute("CREATE TABLE customers(" + "id SERIAL, first_name VARCHAR(255), last_name VARCHAR(255),"
				+ "group_code VARCHAR(4)");
		// グループテーブル削除実行
		jdbcTemplate.execute("DROP TABLE groups IF EXISTS");
		// グループテーブル作成実行
		jdbcTemplate.execute("CREATE TABLE groups(code VARCHAR(4), name VARCHAR(255))");

		// 顧客名の配列をオブジェクト配列（名、姓）のリストに
		List<Object[]> splitUpNames = Arrays.asList("John Woo", "Jeff Dean", "Josh Bloch", "Josh Long").stream()
				.map(name -> name.split(" ")).collect(Collectors.toList());

		// グループの配列をオブジェクト配列（コード、グループ名）のリストに
		List<Object[]> splitUpGroups = Arrays.asList("0001 A", "0002 B", "0003 C", "0004 D").stream()
				.map(name -> name.split(" ")).collect(Collectors.toList());

		// ログを出力
		splitUpNames.forEach(name -> log.info(String.format("Inserting customer record for %s %s", name[0], name[1])));
		splitUpGroups.forEach(object -> log.info(String.format("Inserting group record for %s %s", object[0], object[1])));

		// Uses JdbcTemplate's batchUpdate operation to bulk load data
		jdbcTemplate.batchUpdate("INSERT INTO customers(first_name, last_name) VALUES (?,?)", splitUpNames);
		jdbcTemplate.batchUpdate("INSERT INTO groups(code, 0name) VALUES (?,?)", splitUpGroups);

		log.info("Querying for customer records where first_name = 'Josh':");
		jdbcTemplate.query("SELECT id, first_name, last_name FROM customers WHERE first_name = ?",
				new Object[] { "Josh" },
				(rs, rowNum) -> new Customer(rs.getLong("id"), rs.getString("first_name"), rs.getString("last_name")))
				.forEach(customer -> log.info(customer.toString()));

		RowMapper<Group> rowMapper = new BeanPropertyRowMapper<Group>(Group.class);
		
		Group group	= jdbcTemplate.queryForObject("SELECT code, name FROM groups WHERE code = ?", rowMapper, "0001");
	}
	
	
	public Customer getCustomer(String lastName) {

		RowMapper<Customer> rowMapper = new BeanPropertyRowMapper<Customer>(Customer.class);
		
		Customer customer = jdbcTemplate.queryForObject("SELECT id, first_name, last_name FROM customers WHERE first_name = ?", rowMapper, lastName);
		
		return customer;
	}
	
	public Group getGroup(String code) {

		RowMapper<Group> rowMapper = new BeanPropertyRowMapper<Group>(Group.class);
		
		Group group	= jdbcTemplate.queryForObject("SELECT code, name FROM groups WHERE code = ?", rowMapper, code);
		
		return group;
	}

}
