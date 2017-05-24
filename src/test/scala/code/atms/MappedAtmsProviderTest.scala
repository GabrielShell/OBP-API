package code.atms

import code.api.ServerSetup
import code.atms.Atms.Atm
import code.model.BankId
import net.liftweb.mapper.By

class MappedAtmsProviderTest extends ServerSetup {

  private def delete(): Unit = {
    MappedAtm.bulkDelete_!!()
  }

  override def beforeAll() = {
    super.beforeAll()
    delete()
  }

  override def afterEach() = {
    super.afterEach()
    delete()
  }

  def defaultSetup() =
    new {
      val bankIdX = "some-bank-x"
      val bankIdY = "some-bank-y"

      // 3 atms for bank X (one atm does not have a license)

      val unlicensedAtm = MappedAtm.create
        .mBankId(bankIdX)
        .mName("unlicensed")
        .mAtmId("unlicensed")
        .mCountryCode("es")
        .mPostCode("4444")
        .mLine1("line 1  1 1")
        .mLine2("line 2 2 2 2")
        .mLine3("c4")
        .mCity("d4")
        .mState("e4")
        .mlocationLatitude(4.44)
        .mlocationLongitude(5.55)
        .saveMe()
        // Note: The license is not set


      val atm1 = MappedAtm.create
        .mBankId(bankIdX)
        .mName("atm 1")
        .mAtmId("atm1")
        .mCountryCode("de")
        .mPostCode("123213213")
        .mLine1("a")
        .mLine2("b")
        .mLine3("c")
        .mCity("d")
        .mState("e")
        .mLicenseId("some-license")
        .mLicenseName("Some License")
        .mlocationLatitude(2.22)
        .mlocationLongitude(3.33).saveMe()

      val atm2 = MappedAtm.create
        .mBankId(bankIdX)
        .mName("atm 2")
        .mAtmId("atm2")
        .mCountryCode("fr")
        .mPostCode("898989")
        .mLine1("a2")
        .mLine2("b2")
        .mLine3("c2")
        .mCity("d2")
        .mState("e2")
        .mLicenseId("some-license")
        .mLicenseName("Some License")
        .mlocationLatitude(4.4444)
        .mlocationLongitude(5.5555).saveMe()

    }


  feature("MappedAtmsProvider") {

    scenario("We try to get atms") {

      val fixture = defaultSetup()

      // Only these have license set
      val expectedAtms =  List(fixture.atm1, fixture.atm2)


      Given("the bank in question has atms")
      MappedAtm.find(By(MappedAtm.mBankId, fixture.bankIdX)).isDefined must equal(true)

      When("we try to get the atms for that bank")
      val atmsOpt: Option[List[Atm]] = MappedAtmsProvider.getAtms(BankId(fixture.bankIdX))

      Then("We must get a atms list")
      atmsOpt.isDefined must equal (true)
      val atms = atmsOpt.get

      And("it must contain two atms")
      atms.size must equal(2)

      And("they must be the licensed ones")
      atms must equal (expectedAtms)
    }

    scenario("We try to get atms for a bank that doesn't have any") {

      val fixture = defaultSetup()

      Given("we don't have any atms")

      MappedAtm.find(By(MappedAtm.mBankId, fixture.bankIdY)).isDefined must equal(false)

      When("we try to get the atms for that bank")
      val atmDataOpt = MappedAtmsProvider.getAtms(BankId(fixture.bankIdY))

      Then("we must get back an empty list")
      atmDataOpt.isDefined must equal(true)
      val atms = atmDataOpt.get

      atms.size must equal(0)

    }


    // TODO add test for individual items

  }
}
