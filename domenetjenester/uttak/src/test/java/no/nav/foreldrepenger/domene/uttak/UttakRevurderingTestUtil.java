package no.nav.foreldrepenger.domene.uttak;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittDekningsgradEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;

public class UttakRevurderingTestUtil {

    private static final String ORGNR = KUNSTIG_ORG;
    public static final AktørId AKTØR_ID_MOR = AktørId.dummy();
    public static final AktørId AKTØR_ID_FAR = AktørId.dummy();
    public static final LocalDate FØDSELSDATO = LocalDate.of(2019, 2, 5);
    public static final LocalDate FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK = FØDSELSDATO.plusDays(1);
    public static final LocalDate FØRSTE_UTTAKSDATO_SØKNAD_MOR_FPFF = FØDSELSDATO.minusDays(20);

    private Virksomhet virksomhet;

    private final UttakRepositoryProvider repositoryProvider;

    private final InntektArbeidYtelseTjeneste iayTjeneste;

    public UttakRevurderingTestUtil(UttakRepositoryProvider repositoryProvider,
                                    InntektArbeidYtelseTjeneste iayTjeneste) {
        this.repositoryProvider = repositoryProvider;
        this.iayTjeneste = iayTjeneste;
    }

    public Behandling opprettRevurdering(BehandlingÅrsakType behandlingÅrsakType) {
        return opprettRevurdering(AKTØR_ID_MOR, behandlingÅrsakType);
    }

    public Behandling opprettRevurdering(AktørId aktørId, BehandlingÅrsakType behandlingÅrsakType) {
        List<OppgittPeriodeEntitet> fordeling = new ArrayList<>();
        if (behandlingÅrsakType.equals(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)) {
            fordeling.add(OppgittPeriodeBuilder.ny()
                .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
                .medPeriode(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK.plusDays(10),
                    FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK.plusDays(10))
                .build());
        }
        return opprettRevurdering(aktørId, behandlingÅrsakType, defaultUttaksresultat(),
            new OppgittFordelingEntitet(fordeling, true), OppgittDekningsgradEntitet.bruk100());
    }

    public Behandling opprettEndringssøknadRevurdering(AktørId aktørId,
                                                       LocalDate startDato,
                                                       BehandlingÅrsakType behandlingÅrsakType) {
        List<OppgittPeriodeEntitet> fordeling = new ArrayList<>();
        fordeling.add(OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(startDato, startDato.plusDays(10))
            .build());
        return opprettRevurdering(aktørId, behandlingÅrsakType, defaultUttaksresultat(),
            new OppgittFordelingEntitet(fordeling, true), OppgittDekningsgradEntitet.bruk100());
    }

    private List<UttakResultatPeriodeEntitet> defaultUttaksresultat() {
        return Collections.singletonList(new UttakResultatPeriodeEntitet.Builder(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK,
            FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK.plusDays(10)).medResultatType(PeriodeResultatType.INNVILGET,
            PeriodeResultatÅrsak.UKJENT).build());
    }

    public Behandling opprettRevurdering(AktørId aktørId,
                                         BehandlingÅrsakType behandlingÅrsakType,
                                         List<UttakResultatPeriodeEntitet> opprinneligUttaksResultatPerioder,
                                         OppgittFordelingEntitet nyFordeling,
                                         OppgittDekningsgradEntitet oppgittDekningsgrad) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(aktørId);
        var førstegangsbehandling = byggFørstegangsbehandling(scenario, opprinneligUttaksResultatPerioder);

        var revurderingsscenario = ScenarioMorSøkerForeldrepenger.forFødselUtenSøknad(
            aktørId)
            .medOriginalBehandling(førstegangsbehandling, behandlingÅrsakType)
            .medFordeling(nyFordeling)
            .medOppgittRettighet(new OppgittRettighetEntitet(true, false, false, false))
            .medOppgittDekningsgrad(oppgittDekningsgrad);

        var revurdering = lagre(revurderingsscenario);
        lagreUttaksperiodegrense(revurdering.getId());
        kopierGrunnlagsdata(revurdering);
        repositoryProvider.getFagsakRelasjonRepository()
            .opprettRelasjon(revurdering.getFagsak(), map(oppgittDekningsgrad));
        return revurdering;
    }


    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider, iayTjeneste::lagreIayAggregat);
    }

    private Dekningsgrad map(OppgittDekningsgradEntitet oppgittDekningsgrad) {
        if (oppgittDekningsgrad.getDekningsgrad() == 80) {
            return Dekningsgrad._80;
        }
        if (oppgittDekningsgrad.getDekningsgrad() == 100) {
            return Dekningsgrad._100;
        }
        throw new IllegalArgumentException("Ukjent dekningsgrad " + oppgittDekningsgrad.getDekningsgrad());
    }

    public Behandling opprettRevurderingAdopsjon() {
        var scenario = ScenarioMorSøkerForeldrepenger.forAdopsjon();
        var førstegangsbehandling = byggFørstegangsbehandling(scenario, defaultUttaksresultat());

        var revurderingsscenario = ScenarioMorSøkerForeldrepenger.forAdopsjon()
            .medOriginalBehandling(førstegangsbehandling, BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING);

        var revurdering = revurderingsscenario.lagre(repositoryProvider);
        kopierGrunnlagsdata(revurdering);
        return revurdering;
    }

    private Behandling byggFørstegangsbehandling(ScenarioMorSøkerForeldrepenger scenario,
                                                 List<UttakResultatPeriodeEntitet> perioder) {
        scenario.medDefaultInntektArbeidYtelse();

        scenario.medFordeling(defaultFordeling());

        var uttak = new UttakResultatPerioderEntitet();
        perioder.forEach(p -> uttak.leggTilPeriode(p));
        scenario.medUttak(uttak);

        return lagre(scenario);
    }

    private OppgittFordelingEntitet defaultFordeling() {
        var oppgittPeriodeBuilder = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(FØRSTE_UTTAKSDATO_GJELDENDE_VEDTAK, FØDSELSDATO.plusDays(10));
        return new OppgittFordelingEntitet(Collections.singletonList(oppgittPeriodeBuilder.build()), true);
    }

    private void lagreUttaksperiodegrense(Long behandlingId) {
        var br = repositoryProvider.getBehandlingsresultatRepository().hent(behandlingId);
        var grense = new Uttaksperiodegrense.Builder(br).medFørsteLovligeUttaksdag(LocalDate.of(2018, 1, 1))
            .medMottattDato(FØDSELSDATO)
            .build();
        repositoryProvider.getUttaksperiodegrenseRepository().lagre(behandlingId, grense);
    }

    private void opprettUttakResultat(Behandling førstegangsbehandling, List<UttakResultatPeriodeEntitet> perioder) {
        if (perioder == null || perioder.isEmpty()) {
            return;
        }
        var uttakResultatPerioder = new UttakResultatPerioderEntitet();
        for (var periode : perioder) {
            uttakResultatPerioder.leggTilPeriode(periode);
        }
        repositoryProvider.getFpUttakRepository()
            .lagreOpprinneligUttakResultatPerioder(førstegangsbehandling.getId(), uttakResultatPerioder);
    }

    private void kopierGrunnlagsdata(Behandling revurdering) {
        var originalBehandlingId = revurdering.getOriginalBehandlingId().orElseThrow();
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, revurdering.getId());
    }

    public OppgittFordelingEntitet byggOgLagreOppgittFordelingForMorFPFF(Behandling behandling) {
        var periode1 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(FØRSTE_UTTAKSDATO_SØKNAD_MOR_FPFF, FØRSTE_UTTAKSDATO_SØKNAD_MOR_FPFF.plusWeeks(2))
            .medArbeidsgiver(getVirksomhet());
        var periode2 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(FØRSTE_UTTAKSDATO_SØKNAD_MOR_FPFF.plusWeeks(2).plusDays(1),
                FØRSTE_UTTAKSDATO_SØKNAD_MOR_FPFF.plusWeeks(10))
            .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR));

        var oppgittFordeling = new OppgittFordelingEntitet(
            List.of(periode2.build(), periode1.build()), true);
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medOppgittFordeling(oppgittFordeling);
        ytelsesFordelingRepository.lagre(behandling.getId(), yfBuilder.build());
        return oppgittFordeling;
    }

    public OppgittFordelingEntitet byggOgLagreOppgittFordelingMedPeriode(Behandling behandling,
                                                                         LocalDate fom,
                                                                         LocalDate tom,
                                                                         UttakPeriodeType uttakPeriodeType) {
        var periode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(uttakPeriodeType)
            .medPeriode(fom, tom)
            .medArbeidsgiver(getVirksomhet());

        var oppgittFordeling = new OppgittFordelingEntitet(List.of(periode.build()), true);
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medOppgittFordeling(oppgittFordeling);
        ytelsesFordelingRepository.lagre(behandling.getId(), yfBuilder.build());
        return oppgittFordeling;
    }

    private Arbeidsgiver getVirksomhet() {
        if (virksomhet == null) {
            opprettOgLagreVirksomhet();
        }
        return Arbeidsgiver.virksomhet(ORGNR);
    }

    private void opprettOgLagreVirksomhet() {
        virksomhet = new Virksomhet.Builder().medOrgnr(ORGNR)
            .medNavn("Virksomhet")
            .medRegistrert(FØDSELSDATO.minusYears(10L))
            .medOppstart(FØDSELSDATO.minusYears(10L))
            .build();
    }

    public void opprettInntektsmelding(Behandling revurdering) {
        var journalpostId = new JournalpostId("2");
        var inntektsmeldingBuilder = InntektsmeldingBuilder.builder()
            .medBeløp(BigDecimal.TEN)
            .medStartDatoPermisjon(FØDSELSDATO)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR))
            .medInnsendingstidspunkt(FØDSELSDATO.atStartOfDay())
            .medJournalpostId(journalpostId);

        new InntektsmeldingTjeneste(iayTjeneste).lagreInntektsmelding(revurdering.getFagsak().getSaksnummer(),
            revurdering.getId(), inntektsmeldingBuilder);
    }

    public Behandling byggFørstegangsbehandlingForRevurderingBerørtSak(AktørId aktørId,
                                                                       List<UttakResultatPeriodeEntitet> perioder) {
        return byggFørstegangsbehandlingForRevurderingBerørtSak(aktørId, perioder, null);
    }

    public Behandling byggFørstegangsbehandlingForRevurderingBerørtSak(AktørId aktørId,
                                                                       List<UttakResultatPeriodeEntitet> perioder,
                                                                       Fagsak relatertFagsak) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(aktørId);
        scenario.medDefaultInntektArbeidYtelse();
        var behandlingsresultat = new Behandlingsresultat.Builder().medBehandlingResultatType(
            BehandlingResultatType.INNVILGET).build();
        scenario.medBehandlingsresultat(behandlingsresultat);
        var førstegangsbehandling = lagre(scenario);
        repositoryProvider.getFagsakRelasjonRepository()
            .opprettRelasjon(førstegangsbehandling.getFagsak(), Dekningsgrad._100);
        if (relatertFagsak != null) {
            repositoryProvider.getFagsakRelasjonRepository()
                .kobleFagsaker(førstegangsbehandling.getFagsak(), relatertFagsak, førstegangsbehandling);
        }
        opprettUttakResultat(førstegangsbehandling, perioder);
        return førstegangsbehandling;
    }

    public List<UttakResultatPeriodeEntitet> uttaksresultatBerørtSak(LocalDate fom) {
        return Collections.singletonList(new UttakResultatPeriodeEntitet.Builder(fom, fom.plusDays(10)).medResultatType(
            PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT).build());
    }

    public Behandling opprettRevurderingBerørtSak(AktørId aktørId,
                                                  BehandlingÅrsakType behandlingÅrsakType,
                                                  Behandling førstegangsbehandling) {

        var revurderingsscenario = ScenarioMorSøkerForeldrepenger.forFødselUtenSøknad(
            aktørId)
            .medOriginalBehandling(førstegangsbehandling, behandlingÅrsakType);

        var revurdering = lagre(revurderingsscenario);
        lagreUttaksperiodegrense(revurdering.getId());
        kopierGrunnlagsdata(revurdering);
        return revurdering;
    }

}
