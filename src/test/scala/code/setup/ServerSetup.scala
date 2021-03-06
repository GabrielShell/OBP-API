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

You should have received a copy of the GNU Affero General Public License
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

package code.setup

import java.text.SimpleDateFormat

import code.TestServer
import code.model.BankId
import code.util.Helper.MdcLoggable
import dispatch._
import net.liftweb.json.JsonAST.JObject
import net.liftweb.json.{DefaultFormats, ShortTypeHints}
import org.scalatest._
import _root_.net.liftweb.json.JsonAST.JObject
import net.liftweb.json.JsonDSL._

trait ServerSetup extends FeatureSpec with SendServerRequests
  with BeforeAndAfterEach with GivenWhenThen
  with BeforeAndAfterAll
  with ShouldMatchers with MdcLoggable {

  implicit val formats = DefaultFormats.withHints(ShortTypeHints(List()))
  implicit val dateFormats = net.liftweb.json.DefaultFormats
  
  
  val server = TestServer
  def baseRequest = host(server.host, server.port)
  
  val exampleDateString: String = "22/08/2013"
  val simpleDateFormat: SimpleDateFormat = new SimpleDateFormat("dd/mm/yyyy")
  val exampleDate = simpleDateFormat.parse(exampleDateString)
  
  val mockAccountId1 = "NEW_ACCOUNT_ID_01"
  val mockAccountLabel1 = "NEW_ACCOUNT_LABEL_01"
  
  val mockBankId1 = BankId("testBank1")
  val mockBankId2 = BankId("testBank2")
  
  val mockCustomerNumber1 = "93934903201"
  val mockCustomerNumber2 = "93934903202"
  
  val mockCustomerNumber = "93934903208565488"
  val mockCustomerId = "cba6c9ef-73fa-4032-9546-c6f6496b354a"
  
  val emptyJSON : JObject = ("error" -> "empty List")
  val errorAPIResponse = new APIResponse(400,emptyJSON)
  
}

trait ServerSetupWithTestData extends ServerSetup with DefaultConnectorTestSetup {

  override def beforeEach() = {
    super.beforeEach()
    //create fake data for the tests
    //fake banks
    val banks = createBanks()
    //fake bank accounts
    val accounts = createAccounts(banks)
    //fake transactions
    createTransactions(accounts)
  }

  override def afterEach() = {
    super.afterEach()
    wipeTestData()
  }

}