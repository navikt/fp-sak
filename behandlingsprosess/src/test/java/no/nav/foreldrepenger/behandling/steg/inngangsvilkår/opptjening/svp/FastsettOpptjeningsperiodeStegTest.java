package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.svp;

import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.OPPTJENINGSPERIODEVILKÅR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.OPPTJENINGSVILKÅRET;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingFomKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.konfig.Tid;

@CdiDbAwareTest
class FastsettOpptjeningsperiodeStegTest {

    private static final String ORGNR = "100";
    private final int ANTALL_DAGER_SVANGERSKAP_SKAL_SJEKKE_FOR_OPPTJENING = 28;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private SvangerskapspengerRepository svangerskapspengerRepository;

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;

    @Inject
    @BehandlingTypeRef
    @FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
    private FastsettOpptjeningsperiodeSteg opptjeningsperiodeSvpSteg;
    @Inject
    @BehandlingTypeRef
    @FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
    private VurderOpptjeningsvilkårSteg vurderOpptjeningsvilkårSteg;
    private LocalDate jordmorsdato = LocalDate.now().minusDays(30);

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @Test
    void skal_fastsette_opptjeningsperioden_for_SVP_til_28_dager() {
        // Arrange
        var scenario = byggBehandlingScenario();
        var behandling = lagre(scenario);
        lagreSvp(behandling, jordmorsdato);

        var fagsak = behandling.getFagsak();
        var lås = behandlingRepository.taSkriveLås(behandling.getId());
        // Simulerer at disse vilkårene har blitt opprettet
        opprettVilkår(List.of(OPPTJENINGSPERIODEVILKÅR, OPPTJENINGSVILKÅRET), behandling, lås);
        var kontekst = new BehandlingskontrollKontekst(fagsak.getSaksnummer(), fagsak.getId(), lås);

        // Act
        opptjeningsperiodeSvpSteg.utførSteg(kontekst);

        // Assert
        var opptjening = repositoryProvider.getOpptjeningRepository().finnOpptjening(behandling.getId()).orElseThrow();

        assertThat(opptjening.getFom()).isEqualTo(jordmorsdato.minusDays(28));
        assertThat(opptjening.getTom()).isEqualTo(jordmorsdato.minusDays(1));
        // between tar -> the start date, inclusive, not null & the end date, exclusive, not null
        assertThat(Period.between(opptjening.getFom(), opptjening.getTom()).getDays() + 1)
                .isEqualTo(ANTALL_DAGER_SVANGERSKAP_SKAL_SJEKKE_FOR_OPPTJENING);
    }

    @Test
    void skal_vurdere_opptjeningsvilkåret_for_SVP_til_oppfylt() {
        // Arrange
        var scenario = byggBehandlingScenario();
        var behandling = lagre(scenario);

        var aggregatBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        lagAktørArbeid(aggregatBuilder, behandling.getAktørId(), ORGNR, LocalDate.now().minusYears(1), Tid.TIDENES_ENDE,
                ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        lagAktørInntekt(aggregatBuilder, behandling.getAktørId(), ORGNR, LocalDate.now().minusYears(1), jordmorsdato);
        iayTjeneste.lagreIayAggregat(behandling.getId(), aggregatBuilder);

        lagreSvp(behandling, jordmorsdato);

        var fagsak = behandling.getFagsak();
        var lås = behandlingRepository.taSkriveLås(behandling.getId());
        // Simulerer at disse vilkårene har blitt opprettet
        opprettVilkår(List.of(OPPTJENINGSPERIODEVILKÅR, OPPTJENINGSVILKÅRET), behandling, lås);
        var kontekst = new BehandlingskontrollKontekst(fagsak.getSaksnummer(), fagsak.getId(), lås);
        opptjeningsperiodeSvpSteg.utførSteg(kontekst);

        var lås2 = behandlingRepository.taSkriveLås(behandling.getId());
        var kontekst2 = new BehandlingskontrollKontekst(fagsak.getSaksnummer(), fagsak.getId(), lås2);

        // Act
        vurderOpptjeningsvilkårSteg.utførSteg(kontekst2);

        var behandlingsresultat = repositoryProvider.getBehandlingsresultatRepository().hent(behandling.getId());
        var opptjeningsvilkåret = behandlingsresultat.getVilkårResultat().getVilkårene()
                .stream()
                .filter(v -> v.getVilkårType().equals(OPPTJENINGSVILKÅRET))
                .findFirst()
                .orElseThrow();

        var opptjening = repositoryProvider.getOpptjeningRepository().finnOpptjening(behandling.getId()).orElseThrow();
        assertThat(opptjeningsvilkåret.getGjeldendeVilkårUtfall()).isEqualTo(VilkårUtfallType.OPPFYLT);
        assertThat(opptjening.getOpptjentPeriode().getDays()).isEqualTo(ANTALL_DAGER_SVANGERSKAP_SKAL_SJEKKE_FOR_OPPTJENING);
    }

    @Test
    void skal_vurdere_opptjeningsvilkåret_for_SVP_til_ikke_oppfylt_når_søker_ikke_har_nok_arbeid() {
        // Arrange
        var scenario = byggBehandlingScenario();
        var behandling = lagre(scenario);
        lagreSvp(behandling, jordmorsdato);

        var fagsak = behandling.getFagsak();
        var lås = behandlingRepository.taSkriveLås(behandling.getId());
        // Simulerer at disse vilkårene har blitt opprettet
        opprettVilkår(List.of(OPPTJENINGSPERIODEVILKÅR, OPPTJENINGSVILKÅRET), behandling, lås);
        var kontekst = new BehandlingskontrollKontekst(fagsak.getSaksnummer(), fagsak.getId(), lås);
        opptjeningsperiodeSvpSteg.utførSteg(kontekst);

        var lås2 = behandlingRepository.taSkriveLås(behandling.getId());
        var kontekst2 = new BehandlingskontrollKontekst(fagsak.getSaksnummer(), fagsak.getId(), lås2);

        // Act
        vurderOpptjeningsvilkårSteg.utførSteg(kontekst2);

        var behandlingsresultat = repositoryProvider.getBehandlingsresultatRepository().hent(behandling.getId());
        var opptjeningsvilkåret = behandlingsresultat.getVilkårResultat().getVilkårene()
                .stream()
                .filter(v -> v.getVilkårType().equals(OPPTJENINGSVILKÅRET))
                .findFirst()
                .orElseThrow();

        assertThat(opptjeningsvilkåret.getGjeldendeVilkårUtfall()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
    }

    @Test
    void skal_vurdere_opptjeningsvilkåret_for_SVP_til_oppfylt_når_søker_bare_har_aktivitet() {
        // Arrange
        var scenario = byggBehandlingScenario();
        var behandling = lagre(scenario);
        lagreSvp(behandling, jordmorsdato);

        var fagsak = behandling.getFagsak();
        var lås = behandlingRepository.taSkriveLås(behandling.getId());
        // Simulerer at disse vilkårene har blitt opprettet
        opprettVilkår(List.of(OPPTJENINGSPERIODEVILKÅR, OPPTJENINGSVILKÅRET), behandling, lås);
        var kontekst = new BehandlingskontrollKontekst(fagsak.getSaksnummer(), fagsak.getId(), lås);
        opptjeningsperiodeSvpSteg.utførSteg(kontekst);

        var lås2 = behandlingRepository.taSkriveLås(behandling.getId());
        var kontekst2 = new BehandlingskontrollKontekst(fagsak.getSaksnummer(), fagsak.getId(), lås2);

        // simuler at aktiviteten har blitt godkjent
        var aktivitetsPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(6), LocalDate.now().plusMonths(6));
        var oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(aktivitetsPeriode, ArbeidType.MILITÆR_ELLER_SIVILTJENESTE));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);

        godkjennGittArbeidtypeMedPeriode(behandling, ArbeidType.MILITÆR_ELLER_SIVILTJENESTE, aktivitetsPeriode);

        // Act
        vurderOpptjeningsvilkårSteg.utførSteg(kontekst2);

        var behandlingsresultat = repositoryProvider.getBehandlingsresultatRepository().hent(behandling.getId());
        var opptjeningsvilkåret = behandlingsresultat.getVilkårResultat().getVilkårene()
                .stream()
                .filter(v -> v.getVilkårType().equals(OPPTJENINGSVILKÅRET))
                .findFirst()
                .orElseThrow();

        var opptjening = repositoryProvider.getOpptjeningRepository().finnOpptjening(behandling.getId()).orElseThrow();
        assertThat(opptjeningsvilkåret.getGjeldendeVilkårUtfall()).isEqualTo(VilkårUtfallType.OPPFYLT);
        assertThat(opptjening.getOpptjentPeriode().getDays()).isEqualTo(ANTALL_DAGER_SVANGERSKAP_SKAL_SJEKKE_FOR_OPPTJENING);
    }

    private void godkjennGittArbeidtypeMedPeriode(Behandling behandling, ArbeidType militærEllerSiviltjeneste,
            DatoIntervallEntitet aktivitetsPeriode) {
        var builder = iayTjeneste.opprettBuilderForSaksbehandlet(behandling.getId());
        var aktørArbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        var builderY = aktørArbeidBuilder.getYrkesaktivitetBuilderForType(militærEllerSiviltjeneste);
        var aktivitetsAvtaleBuilder = builderY.getAktivitetsAvtaleBuilder(aktivitetsPeriode, true);
        builderY.leggTilAktivitetsAvtale(aktivitetsAvtaleBuilder);
        aktørArbeidBuilder.leggTilYrkesaktivitet(builderY);
        builder.leggTilAktørArbeid(aktørArbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
    }

    private AbstractTestScenario<?> byggBehandlingScenario() {
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        leggTilSøker(scenario);
        return scenario;
    }

    private void leggTilSøker(AbstractTestScenario<?> scenario) {
        var builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        var søkerAktørId = scenario.getDefaultBrukerAktørId();
        var søker = builderForRegisteropplysninger
                .medPersonas()
                .voksenPerson(søkerAktørId, SivilstandType.UOPPGITT, NavBrukerKjønn.KVINNE)
                .build();
        scenario.medRegisterOpplysninger(søker);
    }

    private void opprettVilkår(List<VilkårType> typeList, Behandling behandling, BehandlingLås skriveLås) {
        // Opprett Vilkårsresultat med vilkårne som skal vurderes, og sett dem som ikke
        // vurdert
        var behandlingsresultat = behandling.getBehandlingsresultat();
        var vilkårBuilder = behandlingsresultat != null
                ? VilkårResultat.builderFraEksisterende(behandlingsresultat.getVilkårResultat())
                : VilkårResultat.builder();
        typeList
                .forEach(vilkårBuilder::leggTilVilkårIkkeVurdert);
        var vilkårResultat = vilkårBuilder.buildFor(behandling);
        behandlingRepository.lagre(vilkårResultat, skriveLås);
    }

    private void lagAktørArbeid(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder, AktørId aktørId, String virksomhetOrgnr,
            LocalDate fom, LocalDate tom, ArbeidType arbeidType) {
        var aktørArbeidBuilder = inntektArbeidYtelseAggregatBuilder
                .getAktørArbeidBuilder(aktørId);
        var opptjeningsnøkkel = Opptjeningsnøkkel.forOrgnummer(virksomhetOrgnr);

        var yrkesaktivitetBuilder = aktørArbeidBuilder
                .getYrkesaktivitetBuilderForNøkkelAvType(opptjeningsnøkkel, arbeidType);
        var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();

        var aktivitetsAvtale = aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(aktivitetsAvtale)
                .medArbeidType(arbeidType)
                .medArbeidsgiver(Arbeidsgiver.virksomhet(virksomhetOrgnr));

        aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        inntektArbeidYtelseAggregatBuilder.leggTilAktørArbeid(aktørArbeidBuilder);
    }

    private void lagAktørInntekt(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder, AktørId aktørId, String virksomhetOrgnr,
            LocalDate fom, LocalDate tom) {
        var aktørInntektBuilder = inntektArbeidYtelseAggregatBuilder
                .getAktørInntektBuilder(aktørId);
        var opptjeningsnøkkel = Opptjeningsnøkkel.forOrgnummer(virksomhetOrgnr);

        var inntektsBuiler = aktørInntektBuilder.getInntektBuilder(InntektsKilde.INNTEKT_OPPTJENING, opptjeningsnøkkel)
                .medArbeidsgiver(Arbeidsgiver.virksomhet(virksomhetOrgnr));
        var inntektsPostBuilder = inntektsBuiler.getInntektspostBuilder()
                .medInntektspostType(InntektspostType.LØNN)
                .medBeløp(BigDecimal.ONE)
                .medPeriode(fom, tom);

        inntektsBuiler.leggTilInntektspost(inntektsPostBuilder);

        aktørInntektBuilder.leggTilInntekt(inntektsBuiler);
        inntektArbeidYtelseAggregatBuilder.leggTilAktørInntekt(aktørInntektBuilder);
    }

    private void lagreSvp(Behandling behandling, LocalDate jordmorsdato) {
        var tilrettelegging = new SvpTilretteleggingEntitet.Builder()
                .medBehovForTilretteleggingFom(jordmorsdato)
                .medIngenTilrettelegging(jordmorsdato, jordmorsdato, SvpTilretteleggingFomKilde.SØKNAD)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(Arbeidsgiver.person(AktørId.dummy()))
                .medMottattTidspunkt(LocalDateTime.now())
                .medKopiertFraTidligereBehandling(false)
                .build();
        var svpGrunnlag = new SvpGrunnlagEntitet.Builder()
                .medBehandlingId(behandling.getId())
                .medOpprinneligeTilrettelegginger(List.of(tilrettelegging))
                .build();
        svangerskapspengerRepository.lagreOgFlush(svpGrunnlag);
    }
}
