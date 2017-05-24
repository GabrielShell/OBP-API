package code.customer

import java.text.SimpleDateFormat
import java.util.Date

import code.api.DefaultUsers
import code.api.util.APIUtil.OAuth._
import code.api.util.ApiRole
import code.api.v1_4_0.JSONFactory1_4_0.CustomerFaceImageJson
import code.api.v1_4_0.V140ServerSetup
import code.api.v2_0_0.CreateCustomerJson
import code.entitlement.Entitlement
import code.model.BankId
import code.model.dataAccess.{MappedBank, ResourceUser}
import code.usercustomerlinks.UserCustomerLink
import net.liftweb.common.{Box, Empty, Full}
import net.liftweb.json.Serialization._

class MappedCustomerProviderTest extends V140ServerSetup with DefaultUsers {

  val exampleDateString: String = "22/08/2013"
  val simpleDateFormat: SimpleDateFormat = new SimpleDateFormat("dd/mm/yyyy")
  val exampleDate = simpleDateFormat.parse(exampleDateString)

  val testBankId1 = BankId("MappedCustomerProviderTest-bank1")
  val testBankId2 = BankId("MappedCustomerProviderTest-bank2")
  val number = "343"

  def createCustomer(bankId: BankId, resourceUser: ResourceUser, nmb: String, user: Some[(Consumer, Token)]) = {
    val customerPostJSON1 = CreateCustomerJson(
                                              user_id = resourceUser.userId,
                                              customer_number = nmb,
                                              legal_name = "Someone",
                                              mobile_phone_number = "125245",
                                              email = "hello@hullo.com",
                                              face_image = CustomerFaceImageJson("www.example.com/person/123/image.png", exampleDate),
                                              date_of_birth = exampleDate,
                                              relationship_status = "Single",
                                              dependants = 1,
                                              dob_of_dependants = List(exampleDate),
                                              highest_education_attained = "Bachelor’s Degree",
                                              employment_status = "Employed",
                                              kyc_status = true,
                                              last_ok_date = exampleDate
                                            )

    When("We create a bank")
    createBank(bankId.value)
    And("We add all required entitlement")
    Entitlement.entitlement.vend.addEntitlement(bankId.value, resourceUser.userId, ApiRole.CanCreateCustomer.toString)
    Entitlement.entitlement.vend.addEntitlement(bankId.value, resourceUser.userId, ApiRole.CanCreateUserCustomerLink.toString)
    And("Try to create a customer")
    val requestPost = (v1_4Request / "banks" / bankId.value / "customer").POST <@ (user)
    val responsePost = makePostRequest(requestPost, write(customerPostJSON1))
    Then("We must get a 200")
    responsePost.code must equal(200)

    val customer: Box[Customer] = Customer.customerProvider.vend.getCustomerByCustomerNumber(nmb, bankId)
    val customerId = customer match {
      case Full(c) => c.customerId
      case Empty => "Empty"
      case _ => "Failure"
    }

    customerId
  }

  feature("Getting customer info") {

    scenario("No customer info exists for user and we try to get it") {
      Given("No MappedCustomer exists for a user")
      When("We try to get it")
      val found = Customer.customerProvider.vend.getCustomerByUserId(testBankId1, authuser2.userId)

      Then("We don't")
      found.isDefined must equal(false)
    }

    scenario("Customer exists and we try to get it") {
      val customerId = createCustomer(testBankId1, authuser1, number, user1)
      Given("MappedCustomer exists for a user")
      When("We try to get it")
      val foundOpt = Customer.customerProvider.vend.getCustomerByUserId(testBankId1, authuser1.userId)

      Then("We do")
      foundOpt.isDefined must equal(true)

      And("It is the right info")
      val found = foundOpt
      found.map(x => x.customerId) must equal(Full(customerId))
    }
  }

  feature("Getting a user from a bankId and customer number") {

    scenario("We try to get a user from a customer number that doesn't exist") {
      val customerNumber = "123213213213213"

      When("We try to get the user for a bank with that customer number")
      val found = Customer.customerProvider.vend.getUser(BankId("some-bank"), customerNumber)

      Then("We must not find a user")
      found.isDefined must equal(false)
    }

    scenario("We try to get a user from a customer number that doesn't exist at the bank in question") {
      val customerNumber = "123213213213213"

      Given("Customer info exists for a different bank")
      val customer2 = createCustomer(testBankId2, authuser1, customerNumber, user1)
      When("We try to get the user for the same bank")
      val user = Customer.customerProvider.vend.getUser(BankId(testBankId2.value), customerNumber)

      Then("We must find a user")
      user.isDefined must equal(true)

      When("We try to get the user for a different bank")
      val found = Customer.customerProvider.vend.getUser(BankId(testBankId2.value + "asdsad"), customerNumber)

      Then("We must not find a user")
      found.isDefined must equal(false)
    }

    scenario("We try to get a user from a customer number that does exist at the bank in question") {
      val customerNumber = "123213213213213"

      When("We check is the customer number available")
      val available = Customer.customerProvider.vend.checkCustomerNumberAvailable(testBankId2, customerNumber)
      Then("We must get positive answer")
      available must equal(true)
      createCustomer(testBankId2, authuser1, customerNumber, user1)
      When("We check is the customer number available after creation")
      val notAvailable = Customer.customerProvider.vend.checkCustomerNumberAvailable(testBankId2, customerNumber)
      Then("We must get negative answer")
      notAvailable must equal(false)

      When("We try to get the user for that bank")
      val found = Customer.customerProvider.vend.getUser(testBankId2, customerNumber)

      Then("We must not find a user")
      found.isDefined must equal(true)
    }

  }


  override def beforeAll() = {
    super.beforeAll()
    MappedBank.bulkDelete_!!()
    Customer.customerProvider.vend.bulkDeleteCustomers()
    UserCustomerLink.userCustomerLink.vend.bulkDeleteUserCustomerLinks()
  }

  override def afterEach() = {
    super.afterEach()
    MappedBank.bulkDelete_!!()
    Customer.customerProvider.vend.bulkDeleteCustomers()
    UserCustomerLink.userCustomerLink.vend.bulkDeleteUserCustomerLinks()
  }
}
