package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import no.nav.folketrygdloven.kalkulus.felles.v1.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.domene.arbeidsforhold.person.PersonIdentTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;

import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdReferanse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.NaturalYtelse;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.NaturalYtelseType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.inntektsmelding.KontaktinformasjonIM;
import no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidInntektsmelding.ArbeidOgInntektsmeldingDtoTjeneste;
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

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(JpaExtension.class)
@ExtendWith(MockitoExtension.class)
class InntektsmeldingDtoTjenesteTest {

    @Mock
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    @Mock
    private FagsakRepository fagsakRepository;
    @Mock
    private BehandlingRepository behandlingRepository;

    @Test
    void henter_im(EntityManager entityManager) {
        var iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);

        var imTjeneste = new InntektsmeldingDtoTjeneste(
            new InntektsmeldingTjeneste(iayTjeneste, new FpInntektsmeldingTjeneste()),
            repositoryProvider.getMottatteDokumentRepository(),
            new VirksomhetTjeneste(), arbeidsgiverTjeneste,
            behandlingRepository,
            fagsakRepository,
            iayTjeneste
        );

        // Sak og behandling
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);

        // Opprett IM fra abakus
        var mottattTidspunkt = LocalDateTime.now().minusWeeks(1);
        var inntekt = new Beløp(400000);
        var refusjon = new Beløp(40000);
        var arbeidsgiver = Arbeidsgiver.virksomhet("123");
        var arbeidsforholdRef = InternArbeidsforholdRef.nyRef();
        var arbeidsgiverOpplysninger = new ArbeidsgiverOpplysninger(arbeidsgiver.getIdentifikator(), "Bedriften");
        var journalpostId = new JournalpostId("456");
        var kontaktInformasjon = new KontaktinformasjonIM("Kontaktersen", "11223344");
        var imBuilder = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(arbeidsforholdRef)
            .medBeløp(inntekt.getVerdi())
            .medRefusjon(refusjon.getVerdi())
            .medJournalpostId(journalpostId);
        var fagsak = behandling.getFagsak();

        // Oppret IAY som trengs for å hente stillingsprosent
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(empty(), VersjonType.REGISTER);
        var periode = DatoIntervallEntitet.fraOgMed(LocalDate.now().minusMonths(10));
        var yrkesAktivitetBuilder = YrkesaktivitetBuilder.oppdatere(empty())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(arbeidsforholdRef)
            .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                .medPeriode(periode)
                .medSisteLønnsendringsdato(periode.getFomDato()))
            .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                .medProsentsats(new Stillingsprosent(80))
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(10), LocalDate.now())));
        var aktørArbeidBuilder = builder.getAktørArbeidBuilder(fagsak.getAktørId()).leggTilYrkesaktivitet(yrkesAktivitetBuilder);
        var iayBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId()).leggTilAktørArbeid(aktørArbeidBuilder);

        // Mock og lagre nødvendige data
        iayTjeneste.lagreInntektsmeldinger(fagsak.getSaksnummer(), behandling.getId(), List.of(imBuilder));
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
        repositoryProvider.getMottatteDokumentRepository().lagre(mottattDokument(behandling, journalpostId, mottattTidspunkt));
        when(fagsakRepository.hentSakGittSaksnummer(any())).thenReturn(Optional.of(fagsak));
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(Optional.of(behandling));
        when(arbeidsgiverTjeneste.hent(arbeidsgiver)).thenReturn(arbeidsgiverOpplysninger);

        var inntektsmeldingerForSak = imTjeneste.hentInntektsmeldingerForSak(fagsak.getSaksnummer());
        assertThat(inntektsmeldingerForSak).hasSize(1);
        var im = inntektsmeldingerForSak.stream().findFirst().get();
        assertThat(im.erAktiv()).isTrue();
        assertThat(im.stillingsprosent()).isEqualTo(new BigDecimal(80));
        assertThat(im.inntektPrMnd()).isEqualTo(inntekt.getVerdi());
        assertThat(im.refusjonPrMnd()).isEqualTo(refusjon.getVerdi());
        assertThat(im.arbeidsgiverNavn()).isEqualTo(arbeidsgiverOpplysninger.getNavn());
        assertThat(im.kontaktpersonNavn()).isNull();
        assertThat(im.kontaktpersonNummer()).isNull();
        assertThat(im.journalpostId()).isEqualTo(journalpostId);
        assertThat(im.mottattTidspunkt()).isEqualTo(mottattTidspunkt);
        assertThat(im.startDatoPermisjon()).isNull();
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
