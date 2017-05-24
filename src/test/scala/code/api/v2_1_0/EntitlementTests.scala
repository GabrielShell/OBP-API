package code.api.v2_1_0


import code.api.util.APIUtil.OAuth._
import code.api.DefaultUsers
import code.api.util.ApiRole.{CanGetEntitlementsForAnyUserAtAnyBank, CanGetEntitlementsForAnyUserAtOneBank}
import code.api.util.ErrorMessages.UserDoesNotHaveRole
import code.entitlement.Entitlement
import code.model.BankId
import net.liftweb.json.JsonAST._
import code.api.util.{ApiRole, ErrorMessages}



/**
 * Created by markom on 10/14/16.
 */
class EntitlementTests extends V210ServerSetup with DefaultUsers {

  val mockBankId = BankId("testBank1")

  override def beforeAll() {
    super.beforeAll()
  }

  override def afterAll() {
    super.afterAll()
  }

  feature("Assuring that endpoint getRoles works as expected - v2.1.0") {

    scenario("We try to get all roles without credentials - getRoles") {
      When("We make the request")
      val requestGet = (v2_1Request / "roles").GET
      val responseGet = makeGetRequest(requestGet)
      Then("We must get a 400")
      responseGet.code must equal(400)
      val error = for { JObject(o) <- responseGet.body; JField("error", JString(error)) <- o } yield error
      And("We must get a message: " + ErrorMessages.UserNotLoggedIn)
      error must contain (ErrorMessages.UserNotLoggedIn)

    }

    scenario("We try to get all roles with credentials - getRoles") {
      When("We make the request")
      val requestGet = (v2_1Request / "roles").GET <@ (user1)
      val responseGet = makeGetRequest(requestGet)
      Then("We must get a 200")
      responseGet.code must equal(200)
    }
  }

  feature("Assuring that endpoint getEntitlementsByBankAndUser works as expected - v2.1.0") {

    scenario("We try to get entitlements without login - getEntitlementsByBankAndUser") {
      When("We make the request")
      val requestGet = (v2_1Request / "banks" / mockBankId.value / "users" / authuser1.userId / "entitlements").GET
      val responseGet = makeGetRequest(requestGet)
      Then("We must get a 400")
      responseGet.code must equal(400)
      val error = for { JObject(o) <- responseGet.body; JField("error", JString(error)) <- o } yield error
      And("We must get a message: " + ErrorMessages.UserNotLoggedIn)
      error must contain (ErrorMessages.UserNotLoggedIn)

    }

    scenario("We try to get entitlements without credentials - getEntitlementsByBankAndUser") {
      When("We make the request")
      val requestGet = (v2_1Request / "banks" / mockBankId.value / "users" / authuser1.userId / "entitlements").GET <@ (user1)
      val responseGet = makeGetRequest(requestGet)
      Then("We must get a 400")
      responseGet.code must equal(400)
      val error = for { JObject(o) <- responseGet.body; JField("error", JString(error)) <- o } yield error
      val requiredEntitlements = CanGetEntitlementsForAnyUserAtOneBank ::
        CanGetEntitlementsForAnyUserAtAnyBank::
        Nil
      val requiredEntitlementsTxt = requiredEntitlements.mkString(" or ")
      And("We must get a message: " + s"$requiredEntitlementsTxt entitlements required")
      error must contain (UserDoesNotHaveRole + requiredEntitlementsTxt)
    }

    scenario("We try to get entitlements with credentials - getEntitlementsByBankAndUser") {
      When("We add required entitlement")
      Entitlement.entitlement.vend.addEntitlement("", authuser1.userId, ApiRole.CanGetEntitlementsForAnyUserAtAnyBank.toString)
      And("We make the request")
      val requestGet = (v2_1Request / "banks" / mockBankId.value / "users" / authuser1.userId / "entitlements").GET <@ (user1)
      val responseGet = makeGetRequest(requestGet)
      Then("We must get a 200")
      responseGet.code must equal(200)
    }
  }
}