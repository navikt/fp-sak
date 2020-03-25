package no.nav.foreldrepenger.behandling.steg.medlemskap;

import static java.util.Arrays.asList;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.steg.medlemskap.fp.KontrollerFaktaLøpendeMedlemskapStegRevurdering;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
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
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.medlem.UtledVurderingsdatoerForMedlemskapTjeneste;
import no.nav.foreldrepenger.domene.medlem.VurderMedlemskapTjeneste;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.db.Repository;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

@RunWith(CdiRunner.class)
public class KontrollerFaktaLøpendeMedlemskapStegRevurderingTest {

    @Rule
    public RepositoryRule repositoryRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider provider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());
    private BehandlingRepository behandlingRepository = provider.getBehandlingRepository();
    private MedlemskapRepository medlemskapRepository = provider.getMedlemskapRepository();
    private PersonopplysningRepository personopplysningRepository = provider.getPersonopplysningRepository();
    private FamilieHendelseRepository familieHendelseRepository = provider.getFamilieHendelseRepository();
    private FagsakRepository fagsakRepository = provider.getFagsakRepository();
    private Repository repository = repositoryRule.getRepository();

    @Inject
    private UtledVurderingsdatoerForMedlemskapTjeneste utlederTjeneste;
    @Inject
    private VurderMedlemskapTjeneste vurderMedlemskapTjeneste;
    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;
    @Inject
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    private KontrollerFaktaLøpendeMedlemskapStegRevurdering steg;

    @Before
    public void setUp() {
        steg = new KontrollerFaktaLøpendeMedlemskapStegRevurdering(utlederTjeneste, provider, vurderMedlemskapTjeneste, skjæringstidspunktTjeneste);
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
        repository.lagre(behandlingsresultat);
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
        repository.lagre(behandlingsresultat);
        oppdaterMedlem(datoMedEndring, periode, revudering.getId());

        // Act
        BehandlingLås lås = behandlingRepository.taSkriveLås(revudering);
        Fagsak fagsak = revudering.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), lås);

        BehandleStegResultat behandleStegResultat = steg.utførSteg(kontekst);

        assertThat(behandleStegResultat.getAksjonspunktListe()).isEmpty();
    }

    private MedlemskapPerioderEntitet opprettPeriode(LocalDate fom, LocalDate tom, MedlemskapDekningType dekningType) {
        MedlemskapPerioderEntitet periode = new MedlemskapPerioderBuilder()
            .medDekningType(dekningType)
            .medMedlemskapType(MedlemskapType.FORELOPIG)
            .medPeriode(fom, tom)
            .medMedlId(1L)
            .build();
        return periode;
    }

    private Behandling opprettRevurdering(Behandling behandling) {
        BehandlingÅrsak.Builder revurderingÅrsak = BehandlingÅrsak.builder(BehandlingÅrsakType.RE_FEIL_ELLER_ENDRET_FAKTA)
            .medOriginalBehandling(behandling);

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
        provider.getUttakRepository().lagreOpprinneligUttakResultatPerioder(behandling.getId(), lagUttaksPeriode());

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
            .medUtbetalingsgrad(BigDecimal.valueOf(100L))
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
