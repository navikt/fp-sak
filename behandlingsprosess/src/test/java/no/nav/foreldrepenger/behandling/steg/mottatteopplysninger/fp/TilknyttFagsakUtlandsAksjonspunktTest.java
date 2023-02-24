package no.nav.foreldrepenger.behandling.steg.mottatteopplysninger.fp;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTOMATISK_MARKERING_AV_UTENLANDSSAK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandling.steg.mottatteopplysninger.TilknyttFagsakSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.mottak.sakskompleks.KobleSakerTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;

@CdiDbAwareTest
public class TilknyttFagsakUtlandsAksjonspunktTest {

    @Mock
    private KobleSakerTjeneste kobleSakerTjeneste;
    @Inject
    private BehandlingRepositoryProvider provider;


    private TilknyttFagsakSteg tilknyttFagsakSteg;

    @Test
    public void utland_markering_dersom_oppgitt_mor_stønad_eøs() {
        // Arrange trinn 1: Behandle søknad om fødsel hvor barn ikke er registrert i TPS
        var fødselsdato = LocalDate.now().minusDays(15); // > 14 dager for å unngå ApDef.VENT_PÅ_FØDSEL
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(fødselsdato).medAntallBarn(1);
        scenario.medOppgittRettighet(new OppgittRettighetEntitet(false, false, false, true, true));
        var behandling = scenario.lagre(provider);

        when(kobleSakerTjeneste.finnFagsakRelasjonDersomOpprettet(behandling)).thenReturn(Optional.of(new FagsakRelasjon(behandling.getFagsak(), null,
                null, null, Dekningsgrad._100, null, fødselsdato.plusYears(3))));
        var kontekst = new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(),
            provider.getBehandlingRepository().taSkriveLås(behandling));

        tilknyttFagsakSteg = new TilknyttFagsakStegImpl(provider, kobleSakerTjeneste, mock(BehandlendeEnhetTjeneste.class), mock(OppgaveTjeneste.class),
            mock(InntektArbeidYtelseTjeneste.class));

        // Act
        var resultat = tilknyttFagsakSteg.utførSteg(kontekst);

        // Assert
        assertThat(resultat.getAksjonspunktListe()).contains(AUTOMATISK_MARKERING_AV_UTENLANDSSAK);
    }
}
