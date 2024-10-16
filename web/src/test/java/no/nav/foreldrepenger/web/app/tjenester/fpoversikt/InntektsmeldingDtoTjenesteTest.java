package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.person.PersonIdentTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;

import no.nav.foreldrepenger.domene.iay.modell.NaturalYtelse;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.NaturalYtelseType;
import no.nav.vedtak.konfig.Tid;

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
        var behandlingRepository = new BehandlingRepository(entityManager);
        var fagsakRepository = new FagsakRepository(entityManager);

        var tjeneste = new InntektsmeldingDtoTjeneste(
            new InntektsmeldingTjeneste(iayTjeneste, new FpInntektsmeldingTjeneste()),
            repositoryProvider.getMottatteDokumentRepository(),
            new VirksomhetTjeneste(),
            new ArbeidsgiverTjeneste(new PersonIdentTjeneste(), new VirksomhetTjeneste()),
            behandlingRepository,
            fagsakRepository,
            new AbakusInMemoryInntektArbeidYtelseTjeneste()
        );

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        var mottattTidspunkt = LocalDateTime.now().minusWeeks(1);
        var inntekt = new Beløp(400000);
        var arbeidsgiver = Arbeidsgiver.virksomhet("123");
        var journalpostId = new JournalpostId("456");
        var imBuilder = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(arbeidsgiver)
            .medBeløp(inntekt.getVerdi())
            .medRefusjon(new Beløp(40000).getVerdi())
            .medJournalpostId(journalpostId);
        var fagsak = behandling.getFagsak();
        iayTjeneste.lagreInntektsmeldinger(fagsak.getSaksnummer(), behandling.getId(), List.of(imBuilder));

        repositoryProvider.getMottatteDokumentRepository().lagre(mottattDokument(behandling, journalpostId, mottattTidspunkt));

        var inntektsmeldingerForSak = tjeneste.hentInntektsmeldingerForSak(fagsak.getSaksnummer());

        assertThat(inntektsmeldingerForSak).hasSize(1);
        var im = inntektsmeldingerForSak.stream().findFirst().get();
        assertThat(im.arbeidsgiverNavn()).isEqualTo(arbeidsgiver.getIdentifikator());
        assertThat(im.mottattTidspunkt()).isEqualTo(mottattTidspunkt);
        assertThat(im.inntektPrMnd()).isEqualTo(inntekt.getVerdi());
        assertThat(im.journalpostId()).isEqualTo(journalpostId);
    }

    @Test
    void konverterAktiveNaturalytelserTilBortfalte() {
        var aktiveNaturalytelser = List.of(
            new NaturalYtelse(Tid.TIDENES_BEGYNNELSE, LocalDate.of(2024, 10, 16), new BigDecimal(1000), NaturalYtelseType.ELEKTRISK_KOMMUNIKASJON),
            new NaturalYtelse(LocalDate.of(2024, 11, 16), LocalDate.of(2024, 11, 20), new BigDecimal(1000), NaturalYtelseType.ELEKTRISK_KOMMUNIKASJON),
            new NaturalYtelse(LocalDate.of(2024, 12, 16), Tid.TIDENES_ENDE, new BigDecimal(1000), NaturalYtelseType.ELEKTRISK_KOMMUNIKASJON)
        );
        var bortfaltePerioder = InntektsmeldingDtoTjeneste.konverterAktivePerioderTilBortfaltePerioder(aktiveNaturalytelser);
        assertThat(bortfaltePerioder).hasSize(2);
        assertThat(bortfaltePerioder.get(0)).isEqualTo(new FpOversiktInntektsmeldingDto.NaturalYtelse(LocalDate.of(2024, 10, 17), LocalDate.of(2024, 11, 15), new BigDecimal(1000), NaturalYtelseType.ELEKTRISK_KOMMUNIKASJON));
        assertThat(bortfaltePerioder.get(1)).isEqualTo(new FpOversiktInntektsmeldingDto.NaturalYtelse(LocalDate.of(2024, 11, 21), LocalDate.of(2024, 12, 15), new BigDecimal(1000), NaturalYtelseType.ELEKTRISK_KOMMUNIKASJON));
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
