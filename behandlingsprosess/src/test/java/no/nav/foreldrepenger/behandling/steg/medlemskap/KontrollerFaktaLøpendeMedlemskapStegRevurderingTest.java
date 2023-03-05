package no.nav.foreldrepenger.behandling.steg.medlemskap;

import static java.util.Arrays.asList;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.revurdering.flytkontroll.BehandlingFlytkontroll;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.steg.medlemskap.fp.KontrollerFaktaLøpendeMedlemskapStegRevurdering;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapDekningType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.medlem.UtledVurderingsdatoerForMedlemskapTjeneste;
import no.nav.foreldrepenger.domene.medlem.VurderMedlemskapTjeneste;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.SkalKopiereUttakTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@CdiDbAwareTest
class KontrollerFaktaLøpendeMedlemskapStegRevurderingTest {

    private final BehandlingRepositoryProvider provider;
    private final BehandlingRepository behandlingRepository;
    private final MedlemskapRepository medlemskapRepository;
    private final PersonopplysningRepository personopplysningRepository;
    private final FamilieHendelseRepository familieHendelseRepository;
    private final FagsakRepository fagsakRepository;
    private final BehandlingsresultatRepository behandlingsresultatRepository;

    @Inject
    private UtledVurderingsdatoerForMedlemskapTjeneste utlederTjeneste;
    @Inject
    private VurderMedlemskapTjeneste vurderMedlemskapTjeneste;
    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;
    @Inject
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    @Inject
    private BehandlingFlytkontroll flytkontroll;
    @Inject
    private SkalKopiereUttakTjeneste skalKopiereUttakTjeneste;
    @Inject
    private UttakInputTjeneste uttakInputTjeneste;

    private KontrollerFaktaLøpendeMedlemskapStegRevurdering steg;

    public KontrollerFaktaLøpendeMedlemskapStegRevurderingTest(EntityManager em) {
        provider = new BehandlingRepositoryProvider(em);
        behandlingRepository = provider.getBehandlingRepository();
        medlemskapRepository = provider.getMedlemskapRepository();
        personopplysningRepository = provider.getPersonopplysningRepository();
        familieHendelseRepository = provider.getFamilieHendelseRepository();
        fagsakRepository = provider.getFagsakRepository();
        behandlingsresultatRepository = provider.getBehandlingsresultatRepository();

    }

    @BeforeEach
    public void setUp() {
        steg = new KontrollerFaktaLøpendeMedlemskapStegRevurdering(utlederTjeneste, provider, vurderMedlemskapTjeneste,
                skjæringstidspunktTjeneste, flytkontroll, skalKopiereUttakTjeneste, uttakInputTjeneste);
    }

    @Test
    void skal_kontrollere_fakta_for_løpende_medlemskap() {
        // Arrange
        var termin = LocalDate.now().plusDays(40); // Default oppsett
        var datoMedEndring = termin;
        var ettÅrSiden = termin.minusYears(1);
        var start = termin.minusWeeks(3);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder()
                .medFørsteUttaksdato(start)
                .build();
        scenario.medAvklarteUttakDatoer(avklarteUttakDatoer);
        scenario.medDefaultFordeling(start);
        scenario.medDefaultSøknadTerminbekreftelse();
        scenario.medDefaultOppgittTilknytning();

        var periode = opprettPeriode(ettÅrSiden, start, MedlemskapDekningType.FTL_2_6);
        scenario.leggTilMedlemskapPeriode(periode);
        var behandling = scenario.lagre(provider);
        avslutterBehandlingOgFagsak(behandling, start);

        var revudering = opprettRevurdering(behandling);

        var inngangsvilkårBuilder = VilkårResultat.builder();
        inngangsvilkårBuilder.leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET);
        var vilkårResultat = inngangsvilkårBuilder.buildFor(revudering);

        var behandlingsresultat = Behandlingsresultat.opprettFor(revudering);
        behandlingsresultat.medOppdatertVilkårResultat(vilkårResultat);
        behandlingRepository.lagre(vilkårResultat, behandlingRepository.taSkriveLås(revudering));
        behandlingsresultatRepository.lagre(revudering.getId(), behandlingsresultat);
        oppdaterMedlem(datoMedEndring, periode, revudering.getId());

        // Act
        var lås = behandlingRepository.taSkriveLås(revudering);
        var fagsak = revudering.getFagsak();
        var kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), lås);

        var behandleStegResultat = steg.utførSteg(kontekst);
        assertThat(behandleStegResultat.getAksjonspunktListe()).containsExactly(AVKLAR_FORTSATT_MEDLEMSKAP);
    }

    @Test
    void skal_ikke_vurdere_løpende_medlemskap_hvis_opprinnelig_medlemskap_er_avslått() {
        // Arrange
        var datoMedEndring = LocalDate.now().plusDays(10);
        var ettÅrSiden = LocalDate.now().minusYears(1);
        var iDag = LocalDate.now();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder()
                .medFørsteUttaksdato(LocalDate.now())
                .build();
        scenario.medAvklarteUttakDatoer(avklarteUttakDatoer);
        scenario.medDefaultSøknadTerminbekreftelse();
        scenario.medDefaultOppgittTilknytning();
        var periode0 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(LocalDate.now(), LocalDate.now().plusWeeks(45))
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123"))
            .build();
        var fordeling = new OppgittFordelingEntitet(List.of(periode0), true);
        scenario.medFordeling(fordeling);

        var periode = opprettPeriode(ettÅrSiden, iDag, MedlemskapDekningType.FTL_2_6);
        scenario.leggTilMedlemskapPeriode(periode);
        var behandling = scenario.lagre(provider);
        avslutterBehandlingOgFagsak(behandling, iDag);

        var revudering = opprettRevurdering(behandling);

        var inngangsvilkårBuilder = VilkårResultat.builder();
        inngangsvilkårBuilder.leggTilVilkårAvslått(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallMerknad.VM_1025);
        var vilkårResultat = inngangsvilkårBuilder.buildFor(revudering);

        var behandlingsresultat = Behandlingsresultat.opprettFor(revudering);
        behandlingsresultat.medOppdatertVilkårResultat(vilkårResultat);
        behandlingRepository.lagre(vilkårResultat, behandlingRepository.taSkriveLås(revudering));
        behandlingsresultatRepository.lagre(revudering.getId(), behandlingsresultat);
        oppdaterMedlem(datoMedEndring, periode, revudering.getId());

        // Act
        var lås = behandlingRepository.taSkriveLås(revudering);
        var fagsak = revudering.getFagsak();
        var kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), lås);

        var behandleStegResultat = steg.utførSteg(kontekst);

        assertThat(behandleStegResultat.getAksjonspunktListe()).isEmpty();
    }

    private MedlemskapPerioderEntitet opprettPeriode(LocalDate fom, LocalDate tom, MedlemskapDekningType dekningType) {
        return new MedlemskapPerioderBuilder()
                .medDekningType(dekningType)
                .medMedlemskapType(MedlemskapType.FORELOPIG)
                .medPeriode(fom, tom)
                .medMedlId(1L)
                .build();
    }

    private Behandling opprettRevurdering(Behandling behandling) {
        var revurderingÅrsak = BehandlingÅrsak.builder(BehandlingÅrsakType.RE_FEIL_ELLER_ENDRET_FAKTA)
                .medOriginalBehandlingId(behandling.getId());

        var revudering = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
                .medBehandlingÅrsak(revurderingÅrsak).build();

        var behandlingId = behandling.getId();
        var revurderingId = behandlingRepository.lagre(revudering, behandlingRepository.taSkriveLås((Long) null));

        medlemskapRepository.kopierGrunnlagFraEksisterendeBehandling(behandlingId, revurderingId);
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandling(behandlingId, revurderingId);
        personopplysningRepository.kopierGrunnlagFraEksisterendeBehandlingUtenVurderinger(behandlingId, revurderingId);
        familieHendelseRepository.kopierGrunnlagFraEksisterendeBehandlingUtenVurderinger(behandlingId, revurderingId);
        return revudering;
    }

    private void oppdaterMedlem(LocalDate datoMedEndring, MedlemskapPerioderEntitet periode, Long behandlingId) {
        var nyPeriode = new MedlemskapPerioderBuilder()
                .medPeriode(datoMedEndring, null)
                .medDekningType(MedlemskapDekningType.FULL)
                .medMedlemskapType(MedlemskapType.ENDELIG)
                .medMedlId(2L)
                .build();
        medlemskapRepository.lagreMedlemskapRegisterOpplysninger(behandlingId, asList(periode, nyPeriode));
    }

    private void avslutterBehandlingOgFagsak(Behandling behandling, LocalDate startdato) {
        var lås = behandlingRepository.taSkriveLås(behandling);
        provider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(behandling.getId(), lagUttaksPeriode(startdato));

        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, lås);
        fagsakRepository.oppdaterFagsakStatus(behandling.getFagsakId(), FagsakStatus.AVSLUTTET);
    }

    private UttakResultatPerioderEntitet lagUttaksPeriode(LocalDate start) {
        var periode = new UttakResultatPeriodeEntitet.Builder(start, start.plusWeeks(53))
                .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
                .build();
        var uttakAktivtet = new UttakAktivitetEntitet.Builder()
                .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
                .medArbeidsforhold(Arbeidsgiver.virksomhet("123"), InternArbeidsforholdRef.nyRef())
                .build();
        var periodeAktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(periode, uttakAktivtet)
                .medUtbetalingsgrad(new Utbetalingsgrad(100))
                .medArbeidsprosent(BigDecimal.valueOf(100L))
                .medErSøktGradering(true)
                .medTrekkonto(StønadskontoType.MØDREKVOTE)
                .build();
        periode.leggTilAktivitet(periodeAktivitet);
        var perioder = new UttakResultatPerioderEntitet();
        perioder.leggTilPeriode(periode);
        return perioder;
    }

}
