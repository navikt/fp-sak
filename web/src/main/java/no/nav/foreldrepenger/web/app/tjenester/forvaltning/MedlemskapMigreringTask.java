package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.hibernate.jpa.HibernateHints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapsvilkårPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VilkårMedlemskap;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VilkårMedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@Dependent
@ProsessTask(value = "medlemskap.migrering", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class MedlemskapMigreringTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MedlemskapMigreringTask.class);

    private static final String FRA_ID = "fraId";

    private final EntityManager entityManager;
    private final ProsessTaskTjeneste prosessTaskTjeneste;
    private final BehandlingRepository behandlingRepository;
    private final MedlemTjeneste medlemTjeneste;
    private final SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private final VilkårMedlemskapRepository vilkårMedlemskapRepository;
    private final VilkårResultatRepository vilkårResultatRepository;
    private final BehandlingsresultatRepository behandlingsresultatRepository;
    private final MedlemskapVilkårPeriodeRepository medlemskapVilkårPeriodeRepository;

    @Inject
    public MedlemskapMigreringTask(EntityManager entityManager,
                                   ProsessTaskTjeneste prosessTaskTjeneste,
                                   BehandlingRepository behandlingRepository,
                                   MedlemTjeneste medlemTjeneste,
                                   SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                   VilkårMedlemskapRepository vilkårMedlemskapRepository,
                                   VilkårResultatRepository vilkårResultatRepository, BehandlingsresultatRepository behandlingsresultatRepository,
                                   MedlemskapVilkårPeriodeRepository medlemskapVilkårPeriodeRepository) {
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.medlemTjeneste = medlemTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.vilkårMedlemskapRepository = vilkårMedlemskapRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.medlemskapVilkårPeriodeRepository = medlemskapVilkårPeriodeRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fraIdProperty = prosessTaskData.getPropertyValue(FRA_ID);
        var fraId = fraIdProperty == null ? null : Long.valueOf(fraIdProperty);

        var behandlinger = finnKandidater(fraId).toList();
        behandlinger.forEach(this::migrerBehandling);

        behandlinger.stream().max(Long::compareTo).ifPresent(nesteId -> prosessTaskTjeneste.lagre(opprettNesteTask(nesteId)));

    }

    private void migrerBehandling(Long behandlingId) {
        LOG.info("Migrerer behandling for {}", behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var vilkårResultat = vilkårResultatRepository.hent(behandlingId);
        switch (behandling.getFagsakYtelseType()) {
            case ENGANGSTØNAD -> {
                var medlemFom = medlemTjeneste.hentMedlemFomDato(behandlingId);
                if (medlemFom.isPresent()) {
                    LOG.info("Medlemfom for {} {}", behandlingId, medlemFom.orElse(null));
                    var vilkårMedlemskap = VilkårMedlemskap.forMedlemFom(vilkårResultat, medlemFom.get());
                    //vilkårMedlemskapRepository.lagre(vilkårMedlemskap);
                }
            }
            case FORELDREPENGER, SVANGERSKAPSPENGER -> {
                var opphørsdato = finnOpphørsdato(behandlingId);
                if (opphørsdato.isPresent()) {
                    var opphørsårsak = hentOpphørsårsak(behandlingId);
                    LOG.info("Opphør for {} {} {} {}", behandling.getFagsakYtelseType(), behandlingId, opphørsdato.get(), opphørsårsak.map(Enum::name).orElse("mangler årsak"));
                    if (opphørsårsak.isPresent() && !Avslagsårsak.UDEFINERT.equals(opphørsårsak.get())) {
                        var vilkårMedlemskap = VilkårMedlemskap.forOpphør(vilkårResultat, opphørsdato.get(), opphørsårsak.get());
                        //vilkårMedlemskapRepository.lagre(vilkårMedlemskap);
                    }
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + behandling.getFagsakYtelseType());
        }
    }

    private Optional<LocalDate> finnOpphørsdato(Long behandlingId) {
        if (behandlingId.equals(2207076L)) {
            return Optional.of(LocalDate.of(2022, 1, 13));
        }
        if (behandlingId.equals(2936458L)) {
            return Optional.of(LocalDate.of(2024, 1, 15));
        }
        if (behandlingId.equals(3121111L)) {
            return Optional.of(LocalDate.of(2024, 5, 20));
        }
        var opphørsdato = hentOpphørsdatoHvisEksisterer(behandlingId);
        if (opphørsdato.isPresent()) {
            var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
            if (opphørsdato.get().isAfter(skjæringstidspunkter.getUtledetSkjæringstidspunkt())) {
                return opphørsdato;
            } else {
                var vilkårResultat = vilkårResultatRepository.hent(behandlingId);
                if (vilkårResultat.getVilkårene().stream().anyMatch(v -> v.getVilkårType() == VilkårType.MEDLEMSKAPSVILKÅRET_LØPENDE && v.getGjeldendeVilkårUtfall() == VilkårUtfallType.IKKE_OPPFYLT)) {
                    LOG.info("Opphørsdato ikke etter stp og løpende vilkår er avslått {} {}", opphørsdato.get(), behandlingId);
                }
            }
        }
        return Optional.empty();
    }

    private Optional<LocalDate> hentOpphørsdatoHvisEksisterer(Long behandlingId) {
        var behandlingsresultatOpt = behandlingsresultatRepository.hentHvisEksisterer(behandlingId);
        if (behandlingsresultatOpt.isEmpty() || behandlingsresultatOpt.get().getVilkårResultat() == null) {
            return Optional.empty();
        }
        var behandlingsresultat = behandlingsresultatOpt.get();
        var medlemskapsvilkåret = behandlingsresultat.getVilkårResultat()
            .getVilkårene()
            .stream()
            .filter(vilkår -> vilkår.getVilkårType().equals(VilkårType.MEDLEMSKAPSVILKÅRET))
            .findFirst();
        if (medlemskapsvilkåret.isEmpty() || medlemskapsvilkåret.get().erIkkeOppfylt()) {
            return Optional.empty();
        }
        var fraVurdering = vilkårMedlemskapRepository.hentHvisEksisterer(behandlingId)
            .flatMap(VilkårMedlemskap::getOpphør);
        if (fraVurdering.isPresent()) {
            return Optional.of(fraVurdering.get().fom());
        }
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var medlemskapVilkårPeriodeGrunnlagEntitet = medlemskapVilkårPeriodeRepository.hentAktivtGrunnlag(behandling);
        if (medlemskapVilkårPeriodeGrunnlagEntitet.isEmpty()) {
            return Optional.empty();
        }
        var løpende = finnVilkår(behandlingsresultat, VilkårType.MEDLEMSKAPSVILKÅRET_LØPENDE);
        if (løpende.isPresent()) {
            var periode = medlemskapVilkårPeriodeGrunnlagEntitet.get().getMedlemskapsvilkårPeriode();
            var overstyringOpt = periode.getOverstyring();
            if (overstyringOpt.getOverstyringsdato().isPresent() && overstyringOpt.getVilkårUtfall().equals(VilkårUtfallType.IKKE_OPPFYLT)) {
                return overstyringOpt.getOverstyringsdato();
            }
            if (løpende.get().erIkkeOppfylt()) {
                return periode.getPerioder().stream()
                    .filter(p -> VilkårUtfallType.IKKE_OPPFYLT.equals(p.getVilkårUtfall()))
                    .map(MedlemskapsvilkårPerioderEntitet::getVurderingsdato)
                    .findFirst();
            }
        }
        return Optional.empty();
    }

    private Optional<Avslagsårsak> hentOpphørsårsak(Long behandlingId) {
        var behandlingsresultat = behandlingsresultatRepository.hent(behandlingId);
        var medlemskapsvilkåret = behandlingsresultat.getVilkårResultat()
            .getVilkårene()
            .stream()
            .filter(vilkår -> vilkår.getVilkårType().gjelderMedlemskap())
            .findFirst();
        if (medlemskapsvilkåret.isEmpty()) {
            return Optional.empty();
        }
        if (medlemskapsvilkåret.get().getGjeldendeVilkårUtfall().equals(VilkårUtfallType.IKKE_OPPFYLT)) {
            return Optional.of(medlemskapsvilkåret.get().getAvslagsårsak());
        }
        var opphørsVurdering = vilkårMedlemskapRepository.hentHvisEksisterer(behandlingId)
            .flatMap(VilkårMedlemskap::getOpphør);
        if (opphørsVurdering.isPresent()) {
            return Optional.of(opphørsVurdering.get().årsak());
        }

        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var medlemskapVilkårPeriodeGrunnlagEntitet = medlemskapVilkårPeriodeRepository.hentAggregatHvisEksisterer(behandling);
        var overstyrtAvslagsårsak = medlemskapVilkårPeriodeGrunnlagEntitet.map(
            vilkårPeriodeGrunnlagEntitet -> vilkårPeriodeGrunnlagEntitet.getMedlemskapsvilkårPeriode().getOverstyring().getAvslagsårsak());
        if (overstyrtAvslagsårsak.isPresent() && !overstyrtAvslagsårsak.get().equals(Avslagsårsak.UDEFINERT)) {
            return overstyrtAvslagsårsak;
        }
        var løpendeVilkår = finnVilkår(behandlingsresultat, VilkårType.MEDLEMSKAPSVILKÅRET_LØPENDE);
        if (løpendeVilkår.isPresent() && løpendeVilkår.get().erIkkeOppfylt()) {
            if (!Avslagsårsak.UDEFINERT.equals(løpendeVilkår.get().getAvslagsårsak())) {
                return Optional.ofNullable(løpendeVilkår.get().getAvslagsårsak());
            }
            var årsakFraMerknad = switch (løpendeVilkår.get().getVilkårUtfallMerknad()) {
                case VM_1020 -> Avslagsårsak.SØKER_ER_IKKE_MEDLEM;
                case VM_1021, VM_1025 -> Avslagsårsak.SØKER_ER_IKKE_BOSATT;
                case VM_1023 -> Avslagsårsak.SØKER_HAR_IKKE_LOVLIG_OPPHOLD;
                case VM_1024 -> Avslagsårsak.SØKER_HAR_IKKE_OPPHOLDSRETT;
                default -> null;
            };
            return Optional.ofNullable(årsakFraMerknad);
        }
        return Optional.empty();
    }

    private Optional<Vilkår> finnVilkår(Behandlingsresultat behandlingsresultat, VilkårType vilkårType) {
        return behandlingsresultat.getVilkårResultat().getVilkårene().stream()
            .filter(vt -> vt.getVilkårType().equals(vilkårType))
            .findFirst();
    }

    private Stream<Long> finnKandidater(Long fraId) {
        var sql = """
            select * from (select distinct(b.id)
                           from fpsak.behandling b
                                    join fpsak.behandling_resultat br on br.behandling_id = b.id
                                    join fpsak.vilkar v on v.VILKAR_RESULTAT_ID = br.INNGANGSVILKAR_RESULTAT_ID
                                    join fpsak.GR_MEDLEMSKAP_VILKAR_PERIODE grm on grm.VILKAR_RESULTAT_ID = br.INNGANGSVILKAR_RESULTAT_ID
                                    join fpsak.MEDLEMSKAP_VILKAR_PERIODE mpe on mpe.ID = grm.MEDLEMSKAP_VILKAR_PERIODE_ID
                                    left outer join FPSAK.MEDLEMSKAP_VILKAR_PERIODER mpr on mpr.MEDLEMSKAP_VILKAR_PERIODE_ID = mpe.ID
                           where aktiv = 'J'
                             and v.VILKAR_TYPE in ('FP_VK_2', 'FP_VK_2_L', 'FP_VK_2_F')
                             and (mpr.vilkar_utfall <> 'OPPFYLT' or mpe.AVSLAGSARSAK <> '-' or mpe.OVERSTYRINGSDATO is not null)
                             and b.ID >:fraId
                           order by b.id
                           )
            where ROWNUM <= 10
            """;

        var query = entityManager.createNativeQuery(sql, Long.class)
            .setParameter("fraId", fraId == null ? 0 : fraId)
            .setHint(HibernateHints.HINT_READ_ONLY, "true");
        return query.getResultStream();
    }

    private static ProsessTaskData opprettNesteTask(Long fraVedtakId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(MedlemskapMigreringTask.class);

        prosessTaskData.setProperty(MedlemskapMigreringTask.FRA_ID, fraVedtakId == null ? null : String.valueOf(fraVedtakId));
        prosessTaskData.setCallIdFraEksisterende();
        return prosessTaskData;
    }
}
