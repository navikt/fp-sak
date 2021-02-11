package no.nav.foreldrepenger.behandling.steg.medlemskap;

import static java.util.Arrays.asList;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.revurdering.flytkontroll.BehandlingFlytkontroll;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.steg.medlemskap.fp.KontrollerFaktaLøpendeMedlemskapStegRevurdering;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
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
public class KontrollerFaktaLøpendeMedlemskapStegRevurderingTest {

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
    public void skal_kontrollere_fakta_for_løpende_medlemskap() {
        // Arrange
        LocalDate datoMedEndring = LocalDate.now().plusDays(10);
        LocalDate ettÅrSiden = LocalDate.now().minusYears(1);
        LocalDate iDag = LocalDate.now();
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        AvklarteUttakDatoerEntitet avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder()
                .medFørsteUttaksdato(LocalDate.now())
                .build();
        scenario.medAvklarteUttakDatoer(avklarteUttakDatoer);
        scenario.medDefaultSøknadTerminbekreftelse();
        scenario.medDefaultOppgittTilknytning();

        MedlemskapPerioderEntitet periode = opprettPeriode(ettÅrSiden, iDag, MedlemskapDekningType.FTL_2_6);
        scenario.leggTilMedlemskapPeriode(periode);
        Behandling behandling = scenario.lagre(provider);
        avslutterBehandlingOgFagsak(behandling);

        Behandling revudering = opprettRevurdering(behandling);

        VilkårResultat.Builder inngangsvilkårBuilder = VilkårResultat.builder();
        inngangsvilkårBuilder.leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT);
        VilkårResultat vilkårResultat = inngangsvilkårBuilder.buildFor(revudering);

        Behandlingsresultat behandlingsresultat = Behandlingsresultat.opprettFor(revudering);
        behandlingsresultat.medOppdatertVilkårResultat(vilkårResultat);
        behandlingRepository.lagre(vilkårResultat, behandlingRepository.taSkriveLås(revudering));
        behandlingsresultatRepository.lagre(revudering.getId(), behandlingsresultat);
        oppdaterMedlem(datoMedEndring, periode, revudering.getId());

        // Act
        BehandlingLås lås = behandlingRepository.taSkriveLås(revudering);
        Fagsak fagsak = revudering.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), lås);

        BehandleStegResultat behandleStegResultat = steg.utførSteg(kontekst);
        assertThat(behandleStegResultat.getAksjonspunktListe()).containsExactly(AVKLAR_FORTSATT_MEDLEMSKAP);
    }

    @Test
    public void skal_ikke_vurdere_løpende_medlemskap_hvis_opprinnelig_medlemskap_er_avslått() {
        // Arrange
        LocalDate datoMedEndring = LocalDate.now().plusDays(10);
        LocalDate ettÅrSiden = LocalDate.now().minusYears(1);
        LocalDate iDag = LocalDate.now();
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        AvklarteUttakDatoerEntitet avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder()
                .medFørsteUttaksdato(LocalDate.now())
                .build();
        scenario.medAvklarteUttakDatoer(avklarteUttakDatoer);
        scenario.medDefaultSøknadTerminbekreftelse();
        scenario.medDefaultOppgittTilknytning();

        MedlemskapPerioderEntitet periode = opprettPeriode(ettÅrSiden, iDag, MedlemskapDekningType.FTL_2_6);
        scenario.leggTilMedlemskapPeriode(periode);
        Behandling behandling = scenario.lagre(provider);
        avslutterBehandlingOgFagsak(behandling);

        Behandling revudering = opprettRevurdering(behandling);

        VilkårResultat.Builder inngangsvilkårBuilder = VilkårResultat.builder();
        inngangsvilkårBuilder.leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.IKKE_OPPFYLT);
        VilkårResultat vilkårResultat = inngangsvilkårBuilder.buildFor(revudering);

        Behandlingsresultat behandlingsresultat = Behandlingsresultat.opprettFor(revudering);
        behandlingsresultat.medOppdatertVilkårResultat(vilkårResultat);
        behandlingRepository.lagre(vilkårResultat, behandlingRepository.taSkriveLås(revudering));
        behandlingsresultatRepository.lagre(revudering.getId(), behandlingsresultat);
        oppdaterMedlem(datoMedEndring, periode, revudering.getId());

        // Act
        BehandlingLås lås = behandlingRepository.taSkriveLås(revudering);
        Fagsak fagsak = revudering.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), lås);

        BehandleStegResultat behandleStegResultat = steg.utførSteg(kontekst);

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
        BehandlingÅrsak.Builder revurderingÅrsak = BehandlingÅrsak.builder(BehandlingÅrsakType.RE_FEIL_ELLER_ENDRET_FAKTA)
                .medOriginalBehandlingId(behandling.getId());

        Behandling revudering = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
                .medBehandlingÅrsak(revurderingÅrsak).build();

        Long behandlingId = behandling.getId();
        Long revurderingId = behandlingRepository.lagre(revudering, behandlingRepository.taSkriveLås((Long) null));

        medlemskapRepository.kopierGrunnlagFraEksisterendeBehandling(behandlingId, revurderingId);
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandling(behandlingId, revurderingId);
        personopplysningRepository.kopierGrunnlagFraEksisterendeBehandlingForRevurdering(behandlingId, revurderingId);
        familieHendelseRepository.kopierGrunnlagFraEksisterendeBehandlingForRevurdering(behandlingId, revurderingId);
        return revudering;
    }

    private void oppdaterMedlem(LocalDate datoMedEndring, MedlemskapPerioderEntitet periode, Long behandlingId) {
        MedlemskapPerioderEntitet nyPeriode = new MedlemskapPerioderBuilder()
                .medPeriode(datoMedEndring, null)
                .medDekningType(MedlemskapDekningType.FULL)
                .medMedlemskapType(MedlemskapType.ENDELIG)
                .medMedlId(2L)
                .build();
        medlemskapRepository.lagreMedlemskapRegisterOpplysninger(behandlingId, asList(periode, nyPeriode));
    }

    private void avslutterBehandlingOgFagsak(Behandling behandling) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        provider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(behandling.getId(), lagUttaksPeriode());

        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, lås);
        fagsakRepository.oppdaterFagsakStatus(behandling.getFagsakId(), FagsakStatus.AVSLUTTET);
    }

    private UttakResultatPerioderEntitet lagUttaksPeriode() {
        LocalDate idag = LocalDate.now();
        UttakResultatPeriodeEntitet periode = new UttakResultatPeriodeEntitet.Builder(idag, idag.plusDays(6))
                .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
                .build();
        UttakAktivitetEntitet uttakAktivtet = new UttakAktivitetEntitet.Builder()
                .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
                .medArbeidsforhold(Arbeidsgiver.virksomhet("123"), InternArbeidsforholdRef.nyRef())
                .build();
        UttakResultatPeriodeAktivitetEntitet periodeAktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(periode, uttakAktivtet)
                .medUtbetalingsgrad(new Utbetalingsgrad(100))
                .medArbeidsprosent(BigDecimal.valueOf(100L))
                .medErSøktGradering(true)
                .medTrekkonto(StønadskontoType.MØDREKVOTE)
                .build();
        periode.leggTilAktivitet(periodeAktivitet);
        UttakResultatPerioderEntitet perioder = new UttakResultatPerioderEntitet();
        perioder.leggTilPeriode(periode);
        return perioder;
    }

}
