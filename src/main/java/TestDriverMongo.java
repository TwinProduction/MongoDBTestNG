import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;
import org.testng.annotations.*;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.Updates.*;
import static org.testng.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;


public class TestDriverMongo {
	
	private static final String MONGO_SERVER_IP   = "localhost";
	private static final int    MONGO_SERVER_PORT = 32768;
	private static final String DATABASE_NAME     = "test";
	private static final String COLLECTION_NAME   = "test";
	
	private MongoClient client = null;
	private MongoDatabase db = null;
	private MongoCollection<Document> collection = null;
	
	
	@BeforeClass
	public void initialSetup() {
		client = new MongoClient(MONGO_SERVER_IP, MONGO_SERVER_PORT);
		db = client.getDatabase(DATABASE_NAME);
		collection = db.getCollection(COLLECTION_NAME);
		collection.drop();
	}
	
	
	@Test
	public void testInsertOne() {
		Document user = new Document("name", "user-01")
			  .append("email", "user-01@example.com")
			  .append("salary", 52000)
			  .append("skills", Arrays.asList("DEV", "QA"));
		Document infoUser = new Document("gender", "M")
			  .append("category", 10)
			  .append("currency", "USD");
		
		user.put("infoUser", infoUser);
		collection.insertOne(user);
		FindIterable<Document> iter = collection.find(eq("name", "user-01"));
		assertEquals(1, iter.into(new ArrayList<Document>()).size());
	}
	
	
	@Test(dependsOnMethods={"testInsertOne"}, groups = {"testInsertMany"})
	public void testInsertManyFromDocumentObjects() {
		Document user2 = new Document("name", "user-02")
			  .append("email", "user-02@example.com")
			  .append("salary", 45000)
			  .append("skills", Arrays.asList("HR"));
		Document infoUser2 = new Document("gender", "F")
			  .append("category", 15)
			  .append("currency", "CAD");
		Document user3 = new Document("name", "user-03")
			  .append("email", "user-03@example.com")
			  .append("salary", 37000)
			  .append("skills", Arrays.asList("SUP"));
		Document infoUser3 = new Document("gender", "M")
			  .append("category", 13)
			  .append("currency", "CAD");
		Document user4 = new Document("name", "user-04")
			  .append("email", "user-04@example.com")
			  .append("salary", 10000)
			  .append("skills", Arrays.asList("DEV", "ART"));
		Document infoUser4 = new Document("gender", "F")
			  .append("category", 11)
			  .append("currency", "EUR");
		
		user2.put("infoUser", infoUser2);
		user3.put("infoUser", infoUser3);
		user4.put("infoUser", infoUser4);
		
		collection.insertMany(Arrays.asList(user2, user3, user4));
		
		assertEquals(4, collection.count());
	}
	
	
	@Test(dependsOnMethods={"testInsertManyFromDocumentObjects"}, groups = {"testInsertMany"})
	public void testInsertManyFromParsedJSON() {
		collection.insertMany(
			  Arrays.asList(
					Document.parse("{name: 'user-05', email: 'user-05@example.com', salary: 75000, "
						  + "skills: ['ART', 'HR'], infoUser: {gender: 'F', category: 33, currency: 'USD'}}"),
					Document.parse("{name: 'user-06', email: 'user-06@example.com', salary: 50000, "
						  + "skills: ['ART', 'QA'], infoUser: {gender: 'M', category: 63, currency: 'CAD'}}"),
					Document.parse("{name: 'user-07', email: 'user-07@example.com', salary: 50000, "
						  + "skills: ['SUP', 'DEV'], infoUser: {gender: 'M', category: 22, currency: 'CAD'}}")
			  )
		);
		assertEquals(7, collection.count());
	}
	
	
	@Test(dependsOnGroups = {"testInsertMany"})
	public void testQuerySalary() { // Amount of a people where 30000 >= salary >= 50000 is true
		FindIterable<Document> mongoIter = collection.find(and(gte("salary", 30000), lte("salary", 50000)));
		assertEquals(4, mongoIter.into(new ArrayList<Document>()).size(),
			  "Wrong number of users with a salary between 30000 and 50000 inclusively");
		printOutput(mongoIter);
	}
	
	
	@Test(dependsOnGroups = {"testInsertMany"})
	public void testQueryGender() { // Amount of females
		FindIterable<Document> mongoIter = collection.find(eq("infoUser.gender", "F"));
		assertEquals(3, mongoIter.into(new ArrayList<Document>()).size(), "Wrong number of female users");
		printOutput(mongoIter);
	}
	
	
	@Test(dependsOnGroups = {"testInsertMany"})
	public void testQuerySkills() { // Amount of people who do not have the "DEV" skill
		FindIterable<Document> mongoIter = collection.find(ne("skills", "DEV"));
		assertEquals(4, mongoIter.into(new ArrayList<Document>()).size(), "Wrong number of users with the skill 'DEV'");
		printOutput(mongoIter);
	}
	
	
	@Test(dependsOnGroups = {"testInsertMany"})
	public void testQueryGetUser01SalaryAndEmail() { // Tests if user-01 exists
		FindIterable<Document> mongoIter = collection.find(eq("name", "user-01"))
			  .projection(include("email", "salary"));
		assertEquals(1, mongoIter.into(new ArrayList<Document>()).size(), "Unable to find user 'user-01'");
		printOutput(mongoIter);
	}
	
	
	@Test(dependsOnGroups = {"testInsertMany"})
	public void testUpdate() {
		collection.updateOne(eq("name", "user-02"), set("salary", 1337));
		FindIterable<Document> mongoIter = collection.find(and(eq("name", "user-02"), eq("salary", 1337)));
		assertEquals(1, mongoIter.into(new ArrayList<Document>()).size(), "Failed to update salary of user-02 to 1337");
		printOutput(mongoIter);
	}
	
	
	@Test(dependsOnGroups = {"testInsertMany"})
	public void testFindUsersNotPaidInUSD() { // Amount of people who are not paid in USD
		FindIterable<Document> mongoIter = collection.find(ne("infoUser.currency", "USD"));
		assertEquals(5, mongoIter.into(new ArrayList<Document>()).size());
		printOutput(mongoIter);
	}
	
	
	@Test(dependsOnGroups = {"testInsertMany"})
	public void testFindUserUsingParsedJSON() {
		FindIterable<Document> mongoIter = collection.find(eq("infoUser",
			  Document.parse("{gender: 'M', category: 22, currency: 'CAD'}")));
		assertEquals(1, mongoIter.into(new ArrayList<Document>()).size());
		printOutput(mongoIter);
	}
	
	
	@Test(dependsOnGroups = {"testInsertMany"})
	public void testFindUsersWithNonExistingField() { //
		FindIterable<Document> mongoIter = collection.find(exists("FIELD_THAT_DOES_NOT_EXIST", true));
		assertEquals(0, mongoIter.into(new ArrayList<Document>()).size(), "Field does not exist, no rows should return");
		printOutput(mongoIter);
	}
	
	
	@Test(dependsOnGroups = {"testInsertMany"})
	public void testFindUsersWithExistingField() { //
		FindIterable<Document> mongoIter = collection.find(exists("name", true));
		assertEquals(collection.count(), mongoIter.into(new ArrayList<Document>()).size());
		printOutput(mongoIter);
	}
	
	
	@Test(dependsOnGroups = {"testInsertMany"})
	public void testFindFemalesAndGetOnlyEmailAndCategory() { //
		FindIterable<Document> mongoIter = collection.find(eq("infoUser.gender", "F"))
			  .projection(include("email", "infoUser.category"));
		assertEquals(3, mongoIter.into(new ArrayList<Document>()).size(), "Wrong number of females found");
		assertFalse((mongoIter.into(new ArrayList<Document>())).get(0).containsKey("name"),
			  "First row contains field 'name', but shouldn't have");
		assertTrue((mongoIter.into(new ArrayList<Document>())).get(0).containsKey("email"),
			  "First row does not contains field 'email', but should have");
		printOutput(mongoIter);
	}
	
	
	@Test(dependsOnGroups = {"testInsertMany"})
	public void testFindAllAndGetOnlyEmailAndCategoryAndLastElementInSkills() { // Naming getting out of hand
		FindIterable<Document> mongoIter = collection.find()
			  .projection(fields(
			  	  include("email", "infoUser.category"),
				    slice("skills", -1)));
		// Projections.slice("skills", 1) would give the first element,
		// Projections.slice("skills", 2) would give the first two elements
		// TODO: assert
		printOutput(mongoIter);
	}
	
	
	@Test(dependsOnGroups = {"testInsertMany"})
	public void testUpdateOne() { // Naming getting out of hand
		collection.updateOne(eq("name", "user-02"), combine(set("salary", 1338), set("infoUser.category", 707)));
		FindIterable<Document> mongoIter = collection.find(eq("name", "user-02"));
		Document testTarget = mongoIter.into(new ArrayList<Document>()).get(0);
		assertTrue((int)testTarget.get("salary") == 1338,
			  "Salary not updated properly");
		assertTrue((int)(((Document)testTarget.get("infoUser")).get("category")) == 707,
			  "Category has not been updated properly");
		printOutput(mongoIter);
	}
	
	
	//////////////////////
	// NON-TEST METHODS //
	//////////////////////
	
	
	public void printOutput(FindIterable<Document> output) {
		for (Document doc : output) {
			System.out.println(doc.toJson());
		}
	}
	
}
