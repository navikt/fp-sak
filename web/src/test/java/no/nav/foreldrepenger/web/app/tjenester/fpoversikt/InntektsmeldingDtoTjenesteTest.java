package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.fpinntektsmelding.FpInntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

@ExtendWith(JpaExtension.class)
class InntektsmeldingDtoTjenesteTest {

    @Test
    void henter_im(EntityManager entityManager) {
        var iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var tjeneste = new InntektsmeldingDtoTjeneste(new InntektsmeldingTjeneste(iayTjeneste, new FpInntektsmeldingTjeneste()), repositoryProvider.getMottatteDokumentRepository());

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        var innsendingstidspunkt = LocalDateTime.now();
        var mottattTidspunkt = LocalDateTime.now().minusWeeks(1);
        var inntekt = new Beløp(400000);
        var arbeidsgiver = Arbeidsgiver.virksomhet("123");
        var journalpostId = new JournalpostId("456");
        var imBuilder = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(arbeidsgiver)
            .medBeløp(inntekt.getVerdi())
            .medRefusjon(new Beløp(40000).getVerdi())
            .medJournalpostId(journalpostId)
            .medInnsendingstidspunkt(innsendingstidspunkt);
        var fagsak = behandling.getFagsak();
        iayTjeneste.lagreInntektsmeldinger(fagsak.getSaksnummer(), behandling.getId(), List.of(imBuilder));

        repositoryProvider.getMottatteDokumentRepository().lagre(mottattDokument(behandling, journalpostId, mottattTidspunkt));

        var inntektsmeldingerForSak = tjeneste.hentInntektsmeldingerForSak(fagsak.getSaksnummer());

        assertThat(inntektsmeldingerForSak).hasSize(1);
        var im = inntektsmeldingerForSak.stream().findFirst().get();
        assertThat(im.arbeidsgiver().identifikator()).isEqualTo(arbeidsgiver.getIdentifikator());
        assertThat(im.innsendingstidspunkt()).isEqualTo(innsendingstidspunkt);
        assertThat(im.mottattTidspunkt()).isEqualTo(mottattTidspunkt);
        assertThat(im.inntektPrMnd()).isEqualTo(inntekt.getVerdi());
        assertThat(im.journalpostId()).isEqualTo(journalpostId);
    }

    private static MottattDokument mottattDokument(Behandling behandling, JournalpostId journalPostId, LocalDateTime mottattTidspunkt) {
        return new MottattDokument.Builder()
            .medJournalPostId(journalPostId)
            .medMottattTidspunkt(mottattTidspunkt)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .build();
    }
}
