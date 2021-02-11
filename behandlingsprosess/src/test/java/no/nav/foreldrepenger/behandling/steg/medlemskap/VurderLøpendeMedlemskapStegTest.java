package no.nav.foreldrepenger.behandling.steg.medlemskap;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapDekningType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapKildeType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapsvilkårPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertLøpendeMedlemskapBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskapPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
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
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.VurderLøpendeMedlemskap;

@CdiDbAwareTest
public class VurderLøpendeMedlemskapStegTest {

    private BehandlingRepositoryProvider provider;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private MedlemskapRepository medlemskapRepository;
    private PersonopplysningRepository personopplysningRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private FagsakRepository fagsakRepository;
    private MedlemskapVilkårPeriodeRepository medlemskapVilkårPeriodeRepository;
    private EntityManager entityManager;

    private VurderLøpendeMedlemskapSteg steg;

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;

    @Inject
    private VurderLøpendeMedlemskap vurdertLøpendeMedlemskapTjeneste;

    @BeforeEach
    public void setUp(EntityManager em) {
        provider = new BehandlingRepositoryProvider(em);
        entityManager = em;
        behandlingRepository = provider.getBehandlingRepository();
        behandlingsresultatRepository = provider.getBehandlingsresultatRepository();
        medlemskapRepository = provider.getMedlemskapRepository();
        personopplysningRepository = provider.getPersonopplysningRepository();
        familieHendelseRepository = provider.getFamilieHendelseRepository();
        fagsakRepository = provider.getFagsakRepository();
        medlemskapVilkårPeriodeRepository = provider.getMedlemskapVilkårPeriodeRepository();
        steg = new VurderLøpendeMedlemskapSteg(vurdertLøpendeMedlemskapTjeneste, provider);
    }

    @Test
    public void skal_gi_avslag() {
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
        MedlemskapPerioderEntitet periode = opprettPeriode(ettÅrSiden, iDag, MedlemskapDekningType.FTL_2_6);
        scenario.leggTilMedlemskapPeriode(periode);

        Behandling behandling = scenario.lagre(provider);
        PersonInformasjonBuilder personInformasjonBuilder = personopplysningRepository.opprettBuilderForRegisterdata(behandling.getId());
        PersonInformasjonBuilder.PersonstatusBuilder personstatusBuilder = personInformasjonBuilder
                .getPersonstatusBuilder(scenario.getDefaultBrukerAktørId(), DatoIntervallEntitet.fraOgMed(ettÅrSiden));
        personstatusBuilder.medPersonstatus(PersonstatusType.BOSA);
        personInformasjonBuilder.leggTil(personstatusBuilder);

        personopplysningRepository.lagre(behandling.getId(), personInformasjonBuilder);
        avslutterBehandlingOgFagsak(behandling);

        Behandling revudering = opprettRevudering(behandling);
        VilkårResultat.Builder inngangsvilkårBuilder = VilkårResultat.builder();
        inngangsvilkårBuilder.leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT);
        VilkårResultat vilkårResultat = inngangsvilkårBuilder.buildFor(revudering);

        Behandlingsresultat behandlingsresultat = Behandlingsresultat.opprettFor(revudering);
        behandlingsresultat.medOppdatertVilkårResultat(vilkårResultat);
        behandlingRepository.lagre(vilkårResultat, behandlingRepository.taSkriveLås(revudering));
        entityManager.persist(behandlingsresultat);
        oppdaterMedlem(datoMedEndring, periode, revudering.getId());

        VurdertMedlemskapPeriodeEntitet.Builder builder = new VurdertMedlemskapPeriodeEntitet.Builder();

        VurdertLøpendeMedlemskapBuilder builderIkkeOk = builder.getBuilderFor(datoMedEndring);
        builderIkkeOk.medBosattVurdering(false);
        builderIkkeOk.medOppholdsrettVurdering(false);
        builderIkkeOk.medLovligOppholdVurdering(false);

        builder.leggTil(builderIkkeOk);

        VurdertMedlemskapPeriodeEntitet hvaSkalLagres = builder.build();
        medlemskapRepository.lagreLøpendeMedlemskapVurdering(revudering.getId(), hvaSkalLagres);

        BehandlingLås lås = behandlingRepository.taSkriveLås(revudering);
        Fagsak fagsak = revudering.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), lås);

        // Act
        steg.utførSteg(kontekst);

        Optional<MedlemskapVilkårPeriodeGrunnlagEntitet> grunnlagOpt = medlemskapVilkårPeriodeRepository.hentAggregatHvisEksisterer(revudering);
        assertThat(grunnlagOpt).isPresent();
        MedlemskapVilkårPeriodeGrunnlagEntitet grunnlag = grunnlagOpt.get();
        List<MedlemskapsvilkårPerioderEntitet> ikkeOppfylt = grunnlag.getMedlemskapsvilkårPeriode().getPerioder().stream()
                .filter(p -> p.getVilkårUtfall().equals(VilkårUtfallType.IKKE_OPPFYLT)).collect(Collectors.toList());
        assertThat(ikkeOppfylt).hasSize(1);
        Behandlingsresultat behandlingsresultat1 = behandlingsresultatRepository.hent(revudering.getId());
        assertThat(behandlingsresultat1.getVilkårResultat().getVilkårene().stream()
                .filter(p -> p.getGjeldendeVilkårUtfall().equals(VilkårUtfallType.IKKE_OPPFYLT) && (p.getAvslagsårsak() != null)).count())
                        .isEqualTo(1);
    }

    private Behandling opprettRevudering(Behandling behandling) {
        BehandlingÅrsak.Builder revurderingÅrsak = BehandlingÅrsak.builder(BehandlingÅrsakType.RE_FEIL_ELLER_ENDRET_FAKTA)
                .medOriginalBehandlingId(behandling.getId());

        Behandling revudering = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
                .medBehandlingÅrsak(revurderingÅrsak).build();
        Long behandlingId = behandling.getId();

        Long revurderingId = behandlingRepository.lagre(revudering, behandlingRepository.taSkriveLås((Long) null));

        Behandlingsresultat.Builder builder = Behandlingsresultat.builderForInngangsvilkår();
        Behandlingsresultat behandlingsresultat = builder.buildFor(revudering);

        behandlingRepository.lagre(behandlingsresultat.getVilkårResultat(), behandlingRepository.taSkriveLås(revurderingId));

        medlemskapRepository.kopierGrunnlagFraEksisterendeBehandling(behandlingId, revurderingId);
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandling(behandlingId, revurderingId);
        personopplysningRepository.kopierGrunnlagFraEksisterendeBehandlingForRevurdering(behandlingId, revurderingId);
        familieHendelseRepository.kopierGrunnlagFraEksisterendeBehandlingForRevurdering(behandlingId, revurderingId);

        return revudering;
    }

    private MedlemskapPerioderEntitet opprettPeriode(LocalDate fom, LocalDate tom, MedlemskapDekningType dekningType) {
        MedlemskapPerioderEntitet periode = new MedlemskapPerioderBuilder()
                .medDekningType(dekningType)
                .medMedlemskapType(MedlemskapType.FORELOPIG)
                .medKildeType(MedlemskapKildeType.MEDL)
                .medPeriode(fom, tom)
                .medMedlId(1L)
                .build();
        return periode;
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

    private void oppdaterMedlem(LocalDate datoMedEndring, MedlemskapPerioderEntitet periode, Long behandlingId) {
        MedlemskapPerioderEntitet nyPeriode = new MedlemskapPerioderBuilder()
                .medPeriode(datoMedEndring, null)
                .medDekningType(MedlemskapDekningType.FULL)
                .medMedlemskapType(MedlemskapType.ENDELIG)
                .medKildeType(MedlemskapKildeType.MEDL)
                .medMedlId(2L)
                .build();
        medlemskapRepository.lagreMedlemskapRegisterOpplysninger(behandlingId, asList(periode, nyPeriode));
    }
}
