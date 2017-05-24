/**
Open Bank Project - API
Copyright (C) 2011-2016, TESOBE Ltd

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You must have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

Email: contact@tesobe.com
TESOBE Ltd
Osloerstrasse 16/17
Berlin 13359, Germany

  This product includes software developed at
  TESOBE (http://www.tesobe.com/)
  by
  Simon Redfern : simon AT tesobe DOT com
  Stefan Bethge : stefan AT tesobe DOT com
  Everett Sochowski : everett AT tesobe DOT com
  Ayoub Benali: ayoub AT tesobe DOT com
  
  */
package code.api.v3_0_0

import _root_.net.liftweb.json.JsonAST.JObject
import _root_.net.liftweb.json.Serialization.write
import code.api.util.APIUtil.OAuth._
import code.api.v1_2._
import code.api.v2_2_0.{AccountsJSONV220, ViewJSONV220, ViewsJSONV220}
import code.api.{APIResponse, DefaultUsers, User1AllPrivileges}
import code.model.{CreateViewJSON, UpdateViewJSON}
import net.liftweb.json.JsonDSL._
import net.liftweb.util.Helpers._
import org.scalatest._

import scala.util.Random._


class API3_0_0Test extends User1AllPrivileges with V300ServerSetup with DefaultUsers {

  implicit val dateFormats = net.liftweb.json.DefaultFormats

  val viewFileds = List(
    "can_see_transaction_this_bank_account","can_see_transaction_other_bank_account",
    "can_see_transaction_metadata","can_see_transaction_label","can_see_transaction_amount",
    "can_see_transaction_type","can_see_transaction_currency","can_see_transaction_start_date",
    "can_see_transaction_finish_date","can_see_transaction_balance","can_see_comments",
    "can_see_narrative","can_see_tags","can_see_images","can_see_bank_account_owners",
    "can_see_bank_account_type","can_see_bank_account_balance","can_see_bank_account_currency",
    "can_see_bank_account_label","can_see_bank_account_national_identifier",
    "can_see_bank_account_swift_bic","can_see_bank_account_iban","can_see_bank_account_number",
    "can_see_bank_account_bank_name","can_see_other_account_national_identifier",
    "can_see_other_account_swift_bic","can_see_other_account_iban",
    "can_see_other_account_bank_name","can_see_other_account_number",
    "can_see_other_account_metadata","can_see_other_account_kind","can_see_more_info",
    "can_see_url","can_see_image_url","can_see_open_corporates_url","can_see_corporate_location",
    "can_see_physical_location","can_see_public_alias","can_see_private_alias","can_add_more_info",
    "can_add_url","can_add_image_url","can_add_open_corporates_url","can_add_corporate_location",
    "can_add_physical_location","can_add_public_alias","can_add_private_alias",
    "can_delete_corporate_location","can_delete_physical_location","can_edit_narrative",
    "can_add_comment","can_delete_comment","can_add_tag","can_delete_tag","can_add_image",
    "can_delete_image","can_add_where_tag","can_see_where_tag","can_delete_where_tag", "can_create_counterparty",
    //V300 New
    "can_see_bank_routing_scheme",
    "can_see_bank_routing_address",
    "can_see_bank_account_routing_scheme",
    "can_see_bank_account_routing_address",
    "can_see_other_bank_routing_scheme",
    "can_see_other_bank_routing_address",
    "can_see_other_account_routing_scheme",
    "can_see_other_account_routing_addres"
    )

  /************************* test tags ************************/

  /**
   * Example: To run tests with tag "getPermissions":
   * 	mvn test -D tagsToInclude
   *
   *  This is made possible by the scalatest maven plugin
   */

  object API3_0 extends Tag("api3.0.0")
  object APIInfo extends Tag("apiInfo")
  object GetHostedBanks extends Tag("hostedBanks")
  object GetHostedBank extends Tag("getHostedBank")
  object GetViews extends Tag("getViews")
  object PostView extends Tag("postView")
  object PutView extends Tag("putView")



  /********************* API test methods ********************/
  val emptyJSON : JObject = ("error" -> "empty List")
  val errorAPIResponse = new APIResponse(400,emptyJSON)


  def randomBankId : String = {
    val banksJson = getBanksInfo.body.extract[BanksJSON]
    val randomPosition = nextInt(banksJson.banks.size)
    val bank = banksJson.banks(randomPosition)
    bank.id
  }

  def randomPrivateAccount(bankId : String) : code.api.v1_2.AccountJSON = {
    val accountsJson = getPrivateAccounts(bankId, user1).body.extract[code.api.v1_2.AccountsJSON].accounts
    val randomPosition = nextInt(accountsJson.size)
    accountsJson(randomPosition)
  }

  def randomViewsIdsToGrant(bankId : String, accountId : String) : List[String]= {
    //get the view ids of the available views on the bank accounts
    val viewsIds = getAccountViews(bankId, accountId, user1).body.extract[ViewsJSONV220].views.map(_.id)
    //choose randomly some view ids to grant
    val (viewsIdsToGrant, _) = viewsIds.splitAt(nextInt(viewsIds.size) + 1)
    viewsIdsToGrant
  }

  def randomView(isPublic: Boolean, alias: String) : CreateViewJSON = {
    CreateViewJSON(
      name = randomString(3),
      description = randomString(3),
      is_public = isPublic,
      which_alias_to_use=alias,
      hide_metadata_if_alias_used=false,
      allowed_actions = viewFileds
    )
  }


  def getAPIInfo : APIResponse = {
    val request = v3_0Request
    makeGetRequest(request)
  }

  def getBanksInfo : APIResponse  = {
    val request = v2_2Request / "banks"
    makeGetRequest(request)
  }

  def getBankInfo(bankId : String) : APIResponse  = {
    val request = v2_2Request / "banks" / bankId
    makeGetRequest(request)
  }

  def getPrivateAccounts(bankId : String, consumerAndToken: Option[(Consumer, Token)]) : APIResponse = {
    val request = v1_2Request / "banks" / bankId / "accounts" / "private" <@(consumerAndToken)
    makeGetRequest(request)
  }

  def getAccountViews(bankId : String, accountId : String, consumerAndToken: Option[(Consumer, Token)]): APIResponse = {
    val request = v3_0Request / "banks" / bankId / "accounts" / accountId / "views" <@(consumerAndToken)
    makeGetRequest(request)
  }

  def postView(bankId: String, accountId: String, view: CreateViewJSON, consumerAndToken: Option[(Consumer, Token)]): APIResponse = {
    val request = (v3_0Request / "banks" / bankId / "accounts" / accountId / "views").POST <@(consumerAndToken)
    makePostRequest(request, write(view))
  }

  def putView(bankId: String, accountId: String, viewId : String, view: UpdateViewJSON, consumerAndToken: Option[(Consumer, Token)]): APIResponse = {
    val request = (v3_0Request / "banks" / bankId / "accounts" / accountId / "views" / viewId).PUT <@(consumerAndToken)
    makePutRequest(request, write(view))
  }



/************************ the tests ************************/
  feature("base line URL works"){
    scenario("we get the api information", API3_0, APIInfo) {
      Given("We will not use an access token")
      When("the request is sent")
      val reply = getAPIInfo
      Then("we must get a 200 ok code")
      reply.code must equal (200)
      val apiInfo = reply.body.extract[APIInfoJSON]
      apiInfo.version must equal ("3.0.0")
    }
  }

  feature("Information about the hosted banks"){
    scenario("we get the hosted banks information", API3_0, GetHostedBanks) {
      Given("We will not use an access token")
      When("the request is sent")
      val reply = getBanksInfo
      Then("we must get a 200 ok code")
      reply.code must equal (200)
      val banksInfo = reply.body.extract[BanksJSON]
      banksInfo.banks.foreach(b => {
        b.id.nonEmpty must equal (true)
      })
    }
  }

  feature("Information about one hosted bank"){
    scenario("we get the hosted bank information", API3_0, GetHostedBank) {
      Given("We will not use an access token")
      When("the request is sent")
      val reply = getBankInfo(randomBankId)
      Then("we must get a 200 ok code")
      reply.code must equal (200)
      val bankInfo = reply.body.extract[code.api.v1_2_1.BankJSON]
      bankInfo.id.nonEmpty must equal (true)
    }

    scenario("we don't get the hosted bank information", API3_0, GetHostedBank) {
      Given("We will not use an access token and request a random bankId")
      When("the request is sent")
      val reply = getBankInfo(randomString(5))
      Then("we must get a 400 code")
      reply.code must equal (400)
      And("we must get an error message")
      reply.body.extract[ErrorMessage].error.nonEmpty must equal (true)
    }
  }

  feature("List of the views of specific bank account - v3.0.0"){
    scenario("We will get the list of the available views on a bank account", API3_0, GetViews) {
      Given("We will use an access token")
      val bankId = randomBankId
      val bankAccount : code.api.v1_2.AccountJSON = randomPrivateAccount(bankId)
      When("the request is sent")
      val reply = getAccountViews(bankId, bankAccount.id, user1)
      Then("we must get a 200 ok code")
      reply.code must equal (200)
      reply.body.extract[ViewsJsonV300]
    }

    scenario("We will not get the list of the available views on a bank account due to missing token", API3_0, GetViews) {
      Given("We will not use an access token")
      val bankId = randomBankId
      val bankAccount : code.api.v1_2.AccountJSON = randomPrivateAccount(bankId)
      When("the request is sent")
      val reply = getAccountViews(bankId, bankAccount.id, None)
      Then("we must get a 400 code")
      reply.code must equal (400)
      And("we must get an error message")
      reply.body.extract[ErrorMessage].error.nonEmpty must equal (true)
    }

    scenario("We will not get the list of the available views on a bank account due to insufficient privileges", API3_0, GetViews) {
      Given("We will use an access token")
      val bankId = randomBankId
      val bankAccount : code.api.v1_2.AccountJSON = randomPrivateAccount(bankId)
      When("the request is sent")
      val reply = getAccountViews(bankId, bankAccount.id, user3)
      Then("we must get a 400 code")
      reply.code must equal (400)
      And("we must get an error message")
      reply.body.extract[ErrorMessage].error.nonEmpty must equal (true)
    }
  }
  feature("Create a view on a bank account - v3.0.0"){
    scenario("we will create a view on a bank account", API3_0, PostView) {
      Given("We will use an access token")
      val bankId = randomBankId
      val bankAccount : code.api.v1_2.AccountJSON = randomPrivateAccount(bankId)
      val viewsBefore = getAccountViews(bankId, bankAccount.id, user1).body.extract[ViewsJsonV300].views
      val view = randomView(true, "")
      When("the request is sent")
      val reply = postView(bankId, bankAccount.id, view, user1)
      Then("we must get a 201 code")
      reply.code must equal (201)
      reply.body.extract[ViewJSONV220]
      And("we must get a new view")
      val viewsAfter = getAccountViews(bankId, bankAccount.id, user1).body.extract[ViewsJsonV300].views
      viewsBefore.size must equal (viewsAfter.size -1)
    }

    scenario("We will not create a view on a bank account due to missing token", API3_0, PostView) {
      Given("We will not use an access token")
      val bankId = randomBankId
      val bankAccount : code.api.v1_2.AccountJSON = randomPrivateAccount(bankId)
      val view = randomView(true, "")
      When("the request is sent")
      val reply = postView(bankId, bankAccount.id, view, None)
      Then("we must get a 400 code")
      reply.code must equal (400)
      And("we must get an error message")
      reply.body.extract[ErrorMessage].error.nonEmpty must equal (true)
    }

    scenario("We will not create a view on a bank account due to insufficient privileges", API3_0, PostView) {
      Given("We will use an access token")
      val bankId = randomBankId
      val bankAccount : code.api.v1_2.AccountJSON = randomPrivateAccount(bankId)
      val view = randomView(true, "")
      When("the request is sent")
      val reply = postView(bankId, bankAccount.id, view, user3)
      Then("we must get a 400 code")
      reply.code must equal (400)
      And("we must get an error message")
      reply.body.extract[ErrorMessage].error.nonEmpty must equal (true)
    }

    scenario("We will not create a view because the bank account does not exist", API3_0, PostView) {
      Given("We will use an access token")
      val bankId = randomBankId
      val view = randomView(true, "")
      When("the request is sent")
      val reply = postView(bankId, randomString(3), view, user1)
      Then("we must get a 400 code")
      reply.code must equal (400)
      And("we must get an error message")
      reply.body.extract[ErrorMessage].error.nonEmpty must equal (true)
    }

    scenario("We will not create a view because the view already exists", API3_0, PostView) {
      Given("We will use an access token")
      val bankId = randomBankId
      val bankAccount : code.api.v1_2.AccountJSON = randomPrivateAccount(bankId)
      val view = randomView(true, "")
      postView(bankId, bankAccount.id, view, user1)
      When("the request is sent")
      val reply = postView(bankId, bankAccount.id, view, user1)
      Then("we must get a 400 code")
      reply.code must equal (400)
      And("we must get an error message")
      reply.body.extract[ErrorMessage].error.nonEmpty must equal (true)
    }
  }

  feature("Update a view on a bank account - v3.0.0") {

    val updatedViewDescription = "aloha"
    val updatedAliasToUse = "public"
    val allowedActions = List("can_see_images", "can_delete_comment")

    def viewUpdateJson(originalView : ViewJsonV300) = {
      //it's not perfect, assumes too much about originalView (i.e. randomView(true, ""))
      UpdateViewJSON(
        description = updatedViewDescription,
        is_public = !originalView.is_public,
        which_alias_to_use = updatedAliasToUse,
        hide_metadata_if_alias_used = !originalView.hide_metadata_if_alias_used,
        allowed_actions = allowedActions
      )
    }

    def someViewUpdateJson() = {
      UpdateViewJSON(
        description = updatedViewDescription,
        is_public = true,
        which_alias_to_use = updatedAliasToUse,
        hide_metadata_if_alias_used = true,
        allowed_actions = allowedActions
      )
    }

    scenario("we will update a view on a bank account", API3_0, PutView) {
      Given("A view exists")
      val bankId = randomBankId
      val bankAccount : code.api.v1_2.AccountJSON = randomPrivateAccount(bankId)
      val view = randomView(true, "")
      val creationReply = postView(bankId, bankAccount.id, view, user1)
      creationReply.code must equal (201)
      val createdView : ViewJsonV300 = creationReply.body.extract[ViewJsonV300]
      createdView.can_see_images must equal(true)
      createdView.can_delete_comment must equal(true)
      createdView.can_delete_physical_location must equal(true)
      createdView.can_edit_owner_comment must equal(true)
      createdView.description must not equal(updatedViewDescription)
      createdView.is_public must equal(true)
      createdView.hide_metadata_if_alias_used must equal(false)

      When("We use a valid access token and valid put json")
      val reply = putView(bankId, bankAccount.id, createdView.id, viewUpdateJson(createdView), user1)
      Then("We must get back the updated view")
      reply.code must equal (200)
      val updatedView = reply.body.extract[ViewJsonV300]
      updatedView.can_see_images must equal(true)
      updatedView.can_delete_comment must equal(true)
      updatedView.can_delete_physical_location must equal(false)
      updatedView.can_edit_owner_comment must equal(false)
      updatedView.description must equal(updatedViewDescription)
      updatedView.is_public must equal(false)
      updatedView.hide_metadata_if_alias_used must equal(true)
    }

    scenario("we will not update a view that doesn't exist", API3_0, PutView) {
      val bankId = randomBankId
      val bankAccount : code.api.v1_2.AccountJSON = randomPrivateAccount(bankId)

      Given("a view does not exist")
      val nonExistantViewId = "asdfasdfasdfasdfasdf"
      val getReply = getAccountViews(bankId, bankAccount.id, user1)
      getReply.code must equal (200)
      val views : ViewsJSONV220 = getReply.body.extract[ViewsJSONV220]
      views.views.foreach(v => v.id must not equal(nonExistantViewId))

      When("we try to update that view")
      val reply = putView(bankId, bankAccount.id, nonExistantViewId, someViewUpdateJson(), user1)
      Then("We must get a 404")
      reply.code must equal(404)
    }

    scenario("We will not update a view on a bank account due to missing token", API3_0, PutView) {
      Given("A view exists")
      val bankId = randomBankId
      val bankAccount : code.api.v1_2.AccountJSON = randomPrivateAccount(bankId)
      val view = randomView(true, "")
      val creationReply = postView(bankId, bankAccount.id, view, user1)
      creationReply.code must equal (201)
      val createdView : ViewJsonV300 = creationReply.body.extract[ViewJsonV300]

      When("we don't use an access token")
      val reply = putView(bankId, bankAccount.id, createdView.id, viewUpdateJson(createdView), None)
      Then("we must get a 400")
      reply.code must equal(400)

      And("we must get an error message")
      reply.body.extract[ErrorMessage].error.nonEmpty must equal (true)
    }

    scenario("we will not update a view on a bank account due to insufficient privileges", API3_0, PutView) {
      Given("A view exists")
      val bankId = randomBankId
      val bankAccount : code.api.v1_2.AccountJSON = randomPrivateAccount(bankId)
      val view = randomView(true, "")
      val creationReply = postView(bankId, bankAccount.id, view, user1)
      creationReply.code must equal (201)
      val createdView : ViewJsonV300 = creationReply.body.extract[ViewJsonV300]

      When("we try to update a view without having sufficient privileges to do so")
      val reply = putView(bankId, bankAccount.id, createdView.id, viewUpdateJson(createdView), user3)
      Then("we must get a 400")
      reply.code must equal(400)

      And("we must get an error message")
      reply.body.extract[ErrorMessage].error.nonEmpty must equal (true)
    }
  }

}
