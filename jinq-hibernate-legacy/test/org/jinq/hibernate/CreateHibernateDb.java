package org.jinq.hibernate;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import org.hibernate.Session;
import org.jinq.hibernate.test.entities.CreditCard;
import org.jinq.hibernate.test.entities.Customer;
import org.jinq.hibernate.test.entities.Item;
import org.jinq.hibernate.test.entities.ItemType;
import org.jinq.hibernate.test.entities.Lineorder;
import org.jinq.hibernate.test.entities.Sale;
import org.jinq.hibernate.test.entities.Supplier;

public class CreateHibernateDb
{
   Session session;

   public CreateHibernateDb(Session session)
   {
      this.session = session;
   }
   
   private Customer createCustomer(String name, String country, int debt, int salary)
   {
      Customer c = new Customer();
      c.setName(name);
      c.setDebt(debt);
      c.setSalary(salary);
      c.setCountry(country);
      return c;
   }
   
   private Sale createSale(Customer customer, int year)
   {
      return createSale(customer, year, 1, 1, 1);
   }

   @SuppressWarnings("deprecation")
   private Sale createSale(Customer customer, int year, int month, int day, int hour)
   {
      Instant instant = LocalDateTime.of(year, month, day, hour, 0).toInstant(ZoneOffset.UTC);
      Date date = Date.from(instant);
      Calendar cal = Calendar.getInstance();
      cal.setTime(date);
      // SQL and JPA has some issues with timezones, so we'll manually
      // set the years and stuff for dates using deprecated methods to 
      // ensure we consistently get certain values in the database.
      java.sql.Date sqlDate = new java.sql.Date(year, month, day);
      java.sql.Time sqlTime = new java.sql.Time(hour, 0, 0);
      java.sql.Timestamp sqlTimestamp = new java.sql.Timestamp(year, month, day, hour, 0, 0, 0);
      
      CreditCard creditCard = new CreditCard();
      creditCard.setName(customer.getName());
      creditCard.setNumber(123456);
      creditCard.setCvv(123);
      
      Sale s = new Sale();
      s.setDate(date);
      s.setCalendar(cal);
      s.setSqlDate(sqlDate);
      s.setSqlTime(sqlTime);
      s.setSqlTimestamp(sqlTimestamp);
      s.setCustomer(customer);
      s.setCreditCard(creditCard);
      return s;
   }

   private Item createItem(String name, ItemType type, int purchasePrice, int salePrice)
   {
      Item i = new Item();
      i.setName(name);
      i.setType(type);
      i.setSaleprice(salePrice);
      i.setPurchaseprice(purchasePrice);
      return i;
   }
   
   private Lineorder addLineorder(Sale s, Item item, int quantity, int transactionConfirmation)
   {
      Lineorder lo = new Lineorder();
      s.addLineorder(lo);
      item.addLineorder(lo);
      lo.setQuantity(quantity);
      lo.setTotal(BigDecimal.valueOf(quantity * item.getPurchaseprice()));
      lo.setTransactionConfirmation(BigInteger.valueOf(transactionConfirmation));
      return lo;
   }
   
   private Supplier createSupplier(String name, String country, long revenue, boolean hasFreeShipping)
   {
      Supplier s = new Supplier();
      s.setName(name);
      s.setCountry(country);
      s.setRevenue(revenue);
      s.setHasFreeShipping(hasFreeShipping);
      s.setSignature(name.getBytes(Charset.forName("UTF-8")));
      s.setSignatureExpiry(Date.from(Instant.now()));
      return s;
   }
   
   void createDatabase()
   {
      session.beginTransaction();

      Customer alice = createCustomer("Alice", "Switzerland", 100, 200);
      Customer bob = createCustomer("Bob", "Switzerland", 200, 300);
      Customer carol = createCustomer("Carol", "USA", 300, 250);
      Customer dave = createCustomer("Dave", "UK", 100, 500);
      Customer eve = createCustomer("Eve", "Canada", 10, 30); 
      session.persist(alice);
      session.persist(bob);
      session.persist(carol);
      session.persist(dave);
      session.persist(eve);

      Supplier hw = createSupplier("HW Supplier", "Canada", 500, false);
      Supplier talentSupply = createSupplier("Talent Agency", "USA", 1000, true);
      Supplier conglomerate = createSupplier("Conglomerate", "Switzerland", 10000000L, false);
      session.persist(hw);
      session.persist(talentSupply);
      session.persist(conglomerate);

      Item widgets = createItem("Widgets", ItemType.SMALL, 5, 10);
      Item wudgets = createItem("Wudgets", ItemType.SMALL, 2, 3);
      Item talent = createItem("Talent", ItemType.OTHER, 6, 1000);
      Item lawnmowers = createItem("Lawnmowers", ItemType.BIG, 100, 102);
      Item screws = createItem("Screws", ItemType.SMALL, 1, 2);
      widgets.setSuppliers(Arrays.asList(hw, conglomerate));
      wudgets.setSuppliers(Arrays.asList(hw));
      talent.setSuppliers(Arrays.asList(talentSupply));
      lawnmowers.setSuppliers(Arrays.asList(conglomerate));
      screws.setSuppliers(Arrays.asList(hw));
      session.persist(widgets);
      session.persist(wudgets);
      session.persist(talent);
      session.persist(lawnmowers);
      session.persist(screws);

      session.flush();
      
      Sale s1 = createSale(alice, 2005, 2, 2, 10);
      Sale s2 = createSale(alice, 2004);
      Sale s3 = createSale(carol, 2003);
      Sale s4 = createSale(carol, 2004, 2, 2, 5);
      Sale s5 = createSale(dave, 2001);
      Sale s6 = createSale(eve, 2005);
      session.persist(s1);
      session.persist(s2);
      session.persist(s3);
      session.persist(s4);
      session.persist(s5);
      session.persist(s6);
      
      session.flush();
      
      session.persist(addLineorder(s1, widgets, 1, 1000));
      session.persist(addLineorder(s2, wudgets, 2, 2000));
      session.persist(addLineorder(s2, screws, 1, 3000));
      session.persist(addLineorder(s2, lawnmowers, 2, 4000));
      session.persist(addLineorder(s3, screws, 1000, 5000));
      session.persist(addLineorder(s4, widgets, 200, 6000));
      session.persist(addLineorder(s5, talent, 6, 7000));
      session.persist(addLineorder(s6, widgets, 2, 8000));
      session.persist(addLineorder(s6, wudgets, 2, 9000));
      session.persist(addLineorder(s6, lawnmowers, 2, 10000));
      session.persist(addLineorder(s6, screws, 7, 11000));

      session.getTransaction().commit();
   }
}
