package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.HendelseVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpAvklartOpphold;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpOppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.PeriodeIkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatArbeidsforholdEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.UtsettelsePeriode;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

@CdiDbAwareTest
class SvpDtoTjenesteTest extends EntityManagerAwareTest {
    private SvpDtoTjeneste tjeneste;
    private BehandlingRepositoryProvider repositoryProvider;
    private SvangerskapspengerRepository svangerskapspengerRepository;
    private BehandlingRepository behandlingRepository;
    @Mock
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    @Inject
    private DtoTjenesteFelles felles;
    @BeforeEach
    public void setUp() {
        var entityManager = getEntityManager();
        svangerskapspengerRepository = new SvangerskapspengerRepository(entityManager);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        tjeneste = new SvpDtoTjeneste(svangerskapspengerRepository, repositoryProvider.getSvangerskapspengerUttakResultatRepository(), felles, inntektsmeldingTjeneste);
    }

    @Test
    void henter_sak_med_svangerskapspenger() {
        var vedtakstidspunkt = LocalDateTime.now();
        var termindato = LocalDate.of(2023, 10, 19);
        var behandling= lagAvsluttetSvpBehandling(termindato, vedtakstidspunkt);
        var mottattDokument = lagDokument(behandling.getFagsakId(), behandling.getId());
        repositoryProvider.getMottatteDokumentRepository().lagre(mottattDokument);

        var tl = lagTilrettelegging(termindato);
        svangerskapspengerRepository.lagreOgFlush(new SvpGrunnlagEntitet.Builder()
            .medBehandlingId(behandling.getId())
            .medOpprinneligeTilrettelegginger(List.of(tl))
            .build());

        var uttakPeriode = lagUttakPeriode(termindato);
        var uttak = lagUttakRes(behandling.getId(), uttakPeriode);
        repositoryProvider.getSvangerskapspengerUttakResultatRepository().lagre(behandling.getId(), uttak);

        var inntektsmelding = lagInntektsmeldingMedferie();
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(BehandlingReferanse.class),any(LocalDate.class))).thenReturn(List.of(inntektsmelding));

        var dto = (SvpSak) tjeneste.hentSak(behandling.getFagsak());

        assertThat(dto.saksnummer()).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat(dto.aktørId()).isEqualTo(behandling.getAktørId().getId());
        assertThat(dto.vedtak()).hasSize(1);
        assertThat(dto.avsluttet()).isFalse();
        var vedtak = dto.vedtak().stream().findFirst().orElseThrow();
        assertThat(vedtak.vedtakstidspunkt()).isEqualTo(vedtakstidspunkt);

        var familieHendelse = dto.familieHendelse();
        assertThat(familieHendelse.fødselsdato()).isNull();
        assertThat(familieHendelse.antallBarn()).isZero();
        assertThat(familieHendelse.termindato()).isEqualTo(termindato);
        assertThat(familieHendelse.omsorgsovertakelse()).isNull();

        assertThat(dto.søknader()).hasSize(1);
        var søknad = dto.søknader().stream().findFirst().get();
        assertThat(søknad.mottattTidspunkt()).isEqualTo(mottattDokument.getMottattTidspunkt());

        assertThat(søknad.tilrettelegginger()).hasSize(1);
        var dtoTl = søknad.tilrettelegginger().stream().findFirst().orElseThrow();
        assertThat(dtoTl.aktivitet().type()).isEqualTo(SvpSak.Aktivitet.Type.FRILANS);
        assertThat(dtoTl.behovFom()).isEqualTo(tl.getBehovForTilretteleggingFom());
        assertThat(dtoTl.risikoFaktorer()).isEqualTo(tl.getOpplysningerOmRisikofaktorer().orElse(null));
        assertThat(dtoTl.tiltak()).isEqualTo(tl.getOpplysningerOmTilretteleggingstiltak().orElse(null));
        assertThat(dtoTl.perioder()).hasSize(1);
        var dtoPeriode = dtoTl.perioder().stream().findFirst().orElseThrow();
        assertThat(dtoPeriode.arbeidstidprosent()).isEqualTo(tl.getTilretteleggingFOMListe().get(0).getStillingsprosent());
        assertThat(dtoPeriode.fom()).isEqualTo(tl.getTilretteleggingFOMListe().get(0).getFomDato());
        assertThat(dtoPeriode.type()).isEqualTo(SvpSak.TilretteleggingType.DELVIS);
        assertThat(dtoTl.oppholdsperioder()).hasSize(2);

        assertThat(vedtak.arbeidsforhold()).hasSize(1);
        var arbeidsforholdUttak = vedtak.arbeidsforhold().stream().findFirst().orElseThrow();
        assertThat(arbeidsforholdUttak.aktivitet().type()).isEqualTo(SvpSak.Aktivitet.Type.FRILANS);
        assertThat(arbeidsforholdUttak.tiltak()).isEqualTo(tl.getOpplysningerOmTilretteleggingstiltak().orElse(null));
        assertThat(arbeidsforholdUttak.risikoFaktorer()).isEqualTo(tl.getOpplysningerOmRisikofaktorer().orElse(null));
        assertThat(arbeidsforholdUttak.ikkeOppfyltÅrsak()).isNull();
        assertThat(arbeidsforholdUttak.svpPerioder()).hasSize(1);
        var svpPeriode = arbeidsforholdUttak.svpPerioder().stream().findFirst().orElseThrow();
        assertThat(svpPeriode.fom()).isEqualTo(uttakPeriode.getFom());
        assertThat(svpPeriode.tom()).isEqualTo(uttakPeriode.getTom());
        assertThat(svpPeriode.utbetalingsgrad()).isEqualTo(uttakPeriode.getUtbetalingsgrad().decimalValue());
        assertThat(svpPeriode.arbeidstidprosent()).isEqualTo(BigDecimal.valueOf(50));
        assertThat(svpPeriode.resultatÅrsak()).isEqualTo(SvpSak.Vedtak.ArbeidsforholdUttak.SvpPeriode.ResultatÅrsak.OPPHØR_OVERGANG_FORELDREPENGER);
        assertThat(svpPeriode.tilretteleggingType()).isEqualTo(SvpSak.TilretteleggingType.DELVIS);
        assertThat(arbeidsforholdUttak.oppholdsperioder()).hasSize(3);
        var oppholdsperiodeListe = arbeidsforholdUttak.oppholdsperioder();
        var fraDatoNå = oppholdsperiodeListe.stream().anyMatch(op-> op.fom().equals(LocalDate.now()));
        var frem25dager = oppholdsperiodeListe.stream().anyMatch(op -> op.tom().equals(LocalDate.now().plusDays(25)));
        var antallKilde = oppholdsperiodeListe.stream().filter(op -> op.kilde().equals(SvpSak.OppholdPeriode.OppholdKilde.SAKSBEHANDLER) || op.kilde().equals(SvpSak.OppholdPeriode.OppholdKilde.INNTEKTSMELDING));
        assertThat(fraDatoNå).isTrue();
        assertThat(frem25dager).isTrue();
        assertThat(antallKilde).hasSize(3);
    }

    private Behandling lagAvsluttetSvpBehandling(LocalDate termindato, LocalDateTime vedtakstidspunkt) {
        var fhBuilder = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.SØKNAD);
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger()
            .medSøknadHendelse(fhBuilder.medTerminbekreftelse(fhBuilder.getTerminbekreftelseBuilder().medTermindato(termindato)));
        scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET).medVedtakstidspunkt(vedtakstidspunkt);
        var behandling = scenario.lagre(repositoryProvider);
        avsluttBehandling(behandling);
        return behandling;
    }

    private Inntektsmelding lagInntektsmeldingMedferie() {
        return InntektsmeldingBuilder.builder()
            .medStartDatoPermisjon(LocalDate.now())
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123456789"))
            .medBeløp(BigDecimal.valueOf(1000))
            .medNærRelasjon(false)
            .medInnsendingstidspunkt(LocalDateTime.now())
            .medJournalpostId(new JournalpostId("987654321"))
            .leggTil(UtsettelsePeriode.ferie(LocalDate.now().plusDays(20), LocalDate.now().plusDays(25)))
            .build();
    }

    private SvangerskapspengerUttakResultatEntitet lagUttakRes(Long behandlingId, SvangerskapspengerUttakResultatPeriodeEntitet uttakPeriode) {
        var behandlingsresultat = repositoryProvider.getBehandlingsresultatRepository().hent(behandlingId);
        return new SvangerskapspengerUttakResultatEntitet.Builder(behandlingsresultat)
            .medUttakResultatArbeidsforhold(new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder().medUttakArbeidType(UttakArbeidType.FRILANS)
                .medPeriode(uttakPeriode)
                .build()).build();
    }

    private SvangerskapspengerUttakResultatPeriodeEntitet lagUttakPeriode(LocalDate termindato) {
        return new SvangerskapspengerUttakResultatPeriodeEntitet.Builder(termindato.minusWeeks(10),
            termindato.minusWeeks(3).minusDays(1)).medUtbetalingsgrad(new Utbetalingsgrad(80))
            .medPeriodeIkkeOppfyltÅrsak(PeriodeIkkeOppfyltÅrsak._8314)
            .medPeriodeResultatType(PeriodeResultatType.INNVILGET)
            .medRegelInput(" ")
            .medRegelEvaluering(" ")
            .build();
    }

    private MottattDokument lagDokument(Long fagsakId, Long behandlingId) {
        return new MottattDokument.Builder().medFagsakId(fagsakId)
            .medBehandlingId(behandlingId)
            .medDokumentType(DokumentTypeId.SØKNAD_SVANGERSKAPSPENGER)
            .medMottattTidspunkt(LocalDateTime.now().minusDays(5))
            .medJournalPostId(new JournalpostId(1L))
            .build();
    }

    private SvpTilretteleggingEntitet lagTilrettelegging(LocalDate termindato) {
        return new SvpTilretteleggingEntitet.Builder()
            .medArbeidType(ArbeidType.FRILANSER)
            .medDelvisTilrettelegging(termindato.minusWeeks(10), BigDecimal.valueOf(50), termindato.minusWeeks(10))
            .medOpplysningerOmRisikofaktorer("risk!")
            .medOpplysningerOmTilretteleggingstiltak("gjort tiltak!")
            .medBehovForTilretteleggingFom(termindato.minusWeeks(12))
            .medMottattTidspunkt(LocalDateTime.now())
            .medKopiertFraTidligereBehandling(false)
            .medAvklarteOpphold(List.of(SvpAvklartOpphold.Builder.nytt().medOppholdPeriode(LocalDate.now(), LocalDate.now().plusDays(4)).medOppholdÅrsak(SvpOppholdÅrsak.FERIE).build(),
                SvpAvklartOpphold.Builder.nytt().medOppholdPeriode(LocalDate.now().plusDays(10), LocalDate.now().plusDays(15)).medOppholdÅrsak(SvpOppholdÅrsak.SYKEPENGER).build()))
            .build();
    }

    private void avsluttBehandling(Behandling behandling) {
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling.getId()));
    }

}
