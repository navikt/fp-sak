package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;

@CdiDbAwareTest
class FaktaLøpendeOmsorgStegTest {

    private static final AktørId FAR_AKTØR_ID = AktørId.dummy();

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
    @BehandlingTypeRef
    private FaktaLøpendeOmsorgSteg steg;

    @Test
    void utførerMedAksjonspunktFaktaForOmsorg() {
        var scenario = opprettBehandlingForFarSomSøker();
        var behandling = scenario.lagre(repositoryProvider);
        var fagsak = behandling.getFagsak();
        var lås = repositoryProvider.getBehandlingRepository().taSkriveLås(behandling);
        var kontekst = new BehandlingskontrollKontekst(fagsak.getSaksnummer(), fagsak.getId(), lås);

        var behandleStegResultat = steg.utførSteg(kontekst);
        assertThat(behandleStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        var aksjonspunkter = behandleStegResultat.getAksjonspunktListe();
        assertThat(aksjonspunkter).hasSize(1);
        assertThat(aksjonspunkter.get(0)).isEqualTo(AksjonspunktDefinisjon.AVKLAR_LØPENDE_OMSORG);

    }

    private static ScenarioFarSøkerForeldrepenger opprettBehandlingForFarSomSøker() {
        var scenario = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(FAR_AKTØR_ID);
        var fødselsdato = LocalDate.now();
        scenario.medSøknadHendelse().medFødselsDato(fødselsdato);
        scenario.medBekreftetHendelse().medFødselsDato(fødselsdato);

        var personInformasjon = scenario
            .opprettBuilderForRegisteropplysninger()
            .medPersonas()
            .mann(FAR_AKTØR_ID, SivilstandType.SAMBOER).statsborgerskap(Landkoder.NOR)
            .build();

        scenario.medRegisterOpplysninger(personInformasjon);

        var rettighet = OppgittRettighetEntitet.beggeRett();
        scenario.medOppgittRettighet(rettighet);
        scenario.medFordeling(new OppgittFordelingEntitet(Collections.singletonList(OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriode(fødselsdato.plusWeeks(8), fødselsdato.plusWeeks(12))
            .build()), true));
        return scenario;
    }

}
