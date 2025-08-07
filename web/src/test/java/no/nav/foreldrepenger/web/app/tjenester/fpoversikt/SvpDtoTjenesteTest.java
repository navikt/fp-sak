package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpAvklartOpphold;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpOppholdKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpOppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingFomKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
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
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.UtsettelsePeriode;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@CdiDbAwareTest
class SvpDtoTjenesteTest extends EntityManagerAwareTest {
    private SvpDtoTjeneste tjeneste;
    private BehandlingRepositoryProvider repositoryProvider;
    private SvangerskapspengerRepository svangerskapspengerRepository;
    private BehandlingRepository behandlingRepository;
    @Mock
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    @Mock
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    @Inject
    private DtoTjenesteFelles felles;
    @Inject
    @FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    private final Arbeidsgiver ARBEIDSGIVER = Arbeidsgiver.virksomhet("123456789");

    @BeforeEach
    public void setUp() {
        var entityManager = getEntityManager();
        svangerskapspengerRepository = new SvangerskapspengerRepository(entityManager);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        tjeneste = new SvpDtoTjeneste(arbeidsgiverTjeneste, svangerskapspengerRepository, repositoryProvider.getSvangerskapspengerUttakResultatRepository(), felles,
            inntektsmeldingTjeneste, skjæringstidspunktTjeneste);
    }

    @Test
    void henter_sak_med_svangerskapspenger() {
        var vedtakstidspunkt = LocalDateTime.now();
        var termindato = LocalDate.of(2023, 10, 19);
        var behandling = lagAvsluttetSvpBehandling(termindato, vedtakstidspunkt);
        var mottattDokument = lagDokument(behandling.getFagsakId(), behandling.getId());
        repositoryProvider.getMottatteDokumentRepository().lagre(mottattDokument);

        var tl = lagTilrettelegging(termindato, ArbeidType.FRILANSER, null, true);
        var tl2 = lagTilrettelegging(termindato, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, ARBEIDSGIVER, false);

        svangerskapspengerRepository.lagreOgFlush(
            new SvpGrunnlagEntitet.Builder().medBehandlingId(behandling.getId()).medOpprinneligeTilrettelegginger(List.of(tl, tl2)).build());

        var uttakPeriode = lagUttakPeriode(termindato);
        var uttakPeriode2 = lagUttakPeriode(termindato);
        var uttak = lagUttakRes(behandling.getId(), uttakPeriode, uttakPeriode2);
        repositoryProvider.getSvangerskapspengerUttakResultatRepository().lagre(behandling.getId(), uttak);

        var inntektsmelding = lagInntektsmeldingMedferie();
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(BehandlingReferanse.class), any(LocalDate.class))).thenReturn(
            List.of(inntektsmelding));

        var dto = (SvpSak) tjeneste.hentSak(behandling.getFagsak());

        assertThat(dto.saksnummer()).isEqualTo(behandling.getSaksnummer().getVerdi());
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

        assertThat(søknad.tilrettelegginger()).hasSize(2);
        //tilrettelegging 1
        var dtoTl = søknad.tilrettelegginger().stream().filter(t-> SvpSak.Aktivitet.Type.ORDINÆRT_ARBEID.equals(t.aktivitet().type())).findFirst().orElseThrow();
        assertThat(dtoTl.aktivitet().type()).isEqualTo(SvpSak.Aktivitet.Type.ORDINÆRT_ARBEID);
        assertThat(dtoTl.behovFom()).isEqualTo(tl.getBehovForTilretteleggingFom());
        assertThat(dtoTl.risikoFaktorer()).isEqualTo(tl.getOpplysningerOmRisikofaktorer().orElse(null));
        assertThat(dtoTl.tiltak()).isEqualTo(tl.getOpplysningerOmTilretteleggingstiltak().orElse(null));
        assertThat(dtoTl.perioder()).hasSize(1);
        var dtoPeriode = dtoTl.perioder().stream().findFirst().orElseThrow();
        assertThat(dtoPeriode.arbeidstidprosent()).isEqualTo(tl.getTilretteleggingFOMListe().get(0).getStillingsprosent());
        assertThat(dtoPeriode.fom()).isEqualTo(tl.getTilretteleggingFOMListe().get(0).getFomDato());
        assertThat(dtoPeriode.type()).isEqualTo(SvpSak.TilretteleggingType.DELVIS);
        //opphold
        assertThat(dtoTl.oppholdsperioder()).isEmpty();


        //tilrettelegging 2
        var dtoT2 = søknad.tilrettelegginger().stream().filter(t-> SvpSak.Aktivitet.Type.FRILANS.equals(t.aktivitet().type())).findFirst().orElseThrow();
        assertThat(dtoT2.aktivitet().type()).isEqualTo(SvpSak.Aktivitet.Type.FRILANS);
        assertThat(dtoT2.behovFom()).isEqualTo(tl.getBehovForTilretteleggingFom());
        assertThat(dtoT2.risikoFaktorer()).isEqualTo(tl.getOpplysningerOmRisikofaktorer().orElse(null));
        assertThat(dtoT2.tiltak()).isEqualTo(tl.getOpplysningerOmTilretteleggingstiltak().orElse(null));
        assertThat(dtoT2.perioder()).hasSize(1);
        var dtoPeriode2 = dtoT2.perioder().stream().findFirst().orElseThrow();
        assertThat(dtoPeriode2.arbeidstidprosent()).isEqualTo(tl.getTilretteleggingFOMListe().get(0).getStillingsprosent());
        assertThat(dtoPeriode2.fom()).isEqualTo(tl.getTilretteleggingFOMListe().get(0).getFomDato());
        assertThat(dtoPeriode2.type()).isEqualTo(SvpSak.TilretteleggingType.DELVIS);
        //opphold
        assertThat(dtoT2.oppholdsperioder()).hasSize(2);
        var opphold1 = dtoT2.oppholdsperioder().stream().filter(o-> SvpSak.OppholdPeriode.Årsak.FERIE.equals(o.årsak())).findFirst().orElseThrow();
        assertThat(opphold1.fom()).isEqualTo(LocalDate.now());
        var opphold2 = dtoT2.oppholdsperioder().stream().filter(o-> SvpSak.OppholdPeriode.Årsak.SYKEPENGER.equals(o.årsak())).findFirst().orElseThrow();
        assertThat(opphold2.fom()).isEqualTo(LocalDate.now().plusDays(10));




        assertThat(vedtak.arbeidsforhold()).hasSize(2);
        var arbeidsforholdUttak1 = vedtak.arbeidsforhold().stream().filter(t-> SvpSak.Aktivitet.Type.FRILANS.equals(t.aktivitet().type())).findFirst().orElseThrow();
        assertThat(arbeidsforholdUttak1.aktivitet().type()).isEqualTo(SvpSak.Aktivitet.Type.FRILANS);
        assertThat(arbeidsforholdUttak1.tiltak()).isEqualTo(tl.getOpplysningerOmTilretteleggingstiltak().orElse(null));
        assertThat(arbeidsforholdUttak1.risikoFaktorer()).isEqualTo(tl.getOpplysningerOmRisikofaktorer().orElse(null));
        assertThat(arbeidsforholdUttak1.ikkeOppfyltÅrsak()).isNull();
        assertThat(arbeidsforholdUttak1.svpPerioder()).hasSize(1);
        var svpPeriode = arbeidsforholdUttak1.svpPerioder().stream().findFirst().orElseThrow();
        assertThat(svpPeriode.fom()).isEqualTo(uttakPeriode.getFom());
        assertThat(svpPeriode.tom()).isEqualTo(uttakPeriode.getTom());
        assertThat(svpPeriode.utbetalingsgrad()).isEqualTo(uttakPeriode.getUtbetalingsgrad().decimalValue());
        assertThat(svpPeriode.arbeidstidprosent()).isEqualTo(BigDecimal.valueOf(50));
        assertThat(svpPeriode.resultatÅrsak()).isEqualTo(SvpSak.Vedtak.ArbeidsforholdUttak.SvpPeriode.ResultatÅrsak.OPPHØR_OVERGANG_FORELDREPENGER);
        assertThat(svpPeriode.tilretteleggingType()).isEqualTo(SvpSak.TilretteleggingType.DELVIS);
        assertThat(arbeidsforholdUttak1.oppholdsperioder()).hasSize(2);

        var arbeidsforholdUttak2 = vedtak.arbeidsforhold().stream().filter(t-> SvpSak.Aktivitet.Type.ORDINÆRT_ARBEID.equals(t.aktivitet().type())).findFirst().orElseThrow();
        assertThat(arbeidsforholdUttak2.oppholdsperioder()).hasSize(1);
    }

    private Behandling lagAvsluttetSvpBehandling(LocalDate termindato, LocalDateTime vedtakstidspunkt) {
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder().medTermindato(termindato));
        scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET).medVedtakstidspunkt(vedtakstidspunkt);
        var behandling = scenario.lagre(repositoryProvider);
        avsluttBehandling(behandling);
        return behandling;
    }

    private Inntektsmelding lagInntektsmeldingMedferie() {
        return InntektsmeldingBuilder.builder()
            .medStartDatoPermisjon(LocalDate.now())
            .medArbeidsgiver(ARBEIDSGIVER)
            .medBeløp(BigDecimal.valueOf(1000))
            .medNærRelasjon(false)
            .medInnsendingstidspunkt(LocalDateTime.now())
            .medJournalpostId(new JournalpostId("987654321"))
            .leggTil(UtsettelsePeriode.ferie(LocalDate.now().plusDays(20), LocalDate.now().plusDays(25)))
            .build();
    }

    private SvangerskapspengerUttakResultatEntitet lagUttakRes(Long behandlingId, SvangerskapspengerUttakResultatPeriodeEntitet uttakPeriode, SvangerskapspengerUttakResultatPeriodeEntitet uttakPeriode2) {
        var behandlingsresultat = repositoryProvider.getBehandlingsresultatRepository().hent(behandlingId);
        return new SvangerskapspengerUttakResultatEntitet.Builder(behandlingsresultat).medUttakResultatArbeidsforhold(
            new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder().medUttakArbeidType(UttakArbeidType.FRILANS)
                .medPeriode(uttakPeriode)
                .build())
            .medUttakResultatArbeidsforhold(
                new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder().medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
                    .medArbeidsforhold(ARBEIDSGIVER, null)
                    .medPeriode(uttakPeriode2)
                    .build()
            ).build();
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

    private SvpTilretteleggingEntitet lagTilrettelegging(LocalDate termindato, ArbeidType arbeidType, Arbeidsgiver arbeidsgiver, boolean skalHaOpphold) {
        var tilrBuilder = new SvpTilretteleggingEntitet.Builder().medArbeidType(arbeidType)
            .medDelvisTilrettelegging(termindato.minusWeeks(10), BigDecimal.valueOf(50), termindato.minusWeeks(10), SvpTilretteleggingFomKilde.SØKNAD)
            .medOpplysningerOmRisikofaktorer("risk!")
            .medOpplysningerOmTilretteleggingstiltak("gjort tiltak!")
            .medBehovForTilretteleggingFom(termindato.minusWeeks(12))
            .medMottattTidspunkt(LocalDateTime.now())
            .medKopiertFraTidligereBehandling(false);

        if (arbeidsgiver != null) {
            tilrBuilder.medArbeidsgiver(arbeidsgiver);
        }

        if (skalHaOpphold) {
            tilrBuilder.medAvklarteOpphold(List.of(SvpAvklartOpphold.Builder.nytt()
                    .medOppholdPeriode(LocalDate.now(), LocalDate.now().plusDays(4))
                    .medOppholdÅrsak(SvpOppholdÅrsak.FERIE)
                    .medKilde(SvpOppholdKilde.REGISTRERT_AV_SAKSBEHANDLER)
                    .build(), SvpAvklartOpphold.Builder.nytt()
                    .medOppholdPeriode(LocalDate.now().plusDays(10), LocalDate.now().plusDays(15))
                    .medOppholdÅrsak(SvpOppholdÅrsak.SYKEPENGER)
                    .medKilde(SvpOppholdKilde.REGISTRERT_AV_SAKSBEHANDLER)
                    .build()));
        }
        return tilrBuilder.build();
    }

    private void avsluttBehandling(Behandling behandling) {
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling.getId()));
    }

}
