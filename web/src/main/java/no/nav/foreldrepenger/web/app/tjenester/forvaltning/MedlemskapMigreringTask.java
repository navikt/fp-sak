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

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
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
    private final BehandlingsresultatRepository behandlingsresultatRepository;
    private final MedlemskapVilkårPeriodeRepository medlemskapVilkårPeriodeRepository;

    @Inject
    public MedlemskapMigreringTask(EntityManager entityManager,
                                   ProsessTaskTjeneste prosessTaskTjeneste,
                                   BehandlingRepository behandlingRepository,
                                   MedlemTjeneste medlemTjeneste,
                                   BehandlingsresultatRepository behandlingsresultatRepository,
                                   MedlemskapVilkårPeriodeRepository medlemskapVilkårPeriodeRepository) {
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.medlemTjeneste = medlemTjeneste;
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

        //TODO lagre nytt repo
        switch (behandling.getFagsakYtelseType()) {
            case ENGANGSTØNAD -> {
                var medlemFom = medlemTjeneste.hentMedlemFomDato(behandlingId);
                LOG.info("Medlemfom for {} {}", behandlingId, medlemFom.orElse(null));
            }
            case FORELDREPENGER, SVANGERSKAPSPENGER -> {
                var opphørsdato = finnOpphørsdato(behandlingId);
                LOG.info("Opphørsdato for {} {} {}", behandling.getFagsakYtelseType(), behandlingId, opphørsdato.orElse(null));
                if (opphørsdato.isPresent()) {
                    var opphørsårsak = medlemTjeneste.hentAvslagsårsak(behandlingId);
                    LOG.info("Opphørsårsak for {} {} {}", behandling.getFagsakYtelseType(), behandlingId, opphørsårsak.orElse(null));
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + behandling.getFagsakYtelseType());
        }
    }

    public Optional<LocalDate> finnOpphørsdato(Long behandlingId) {
        var behandlingsresultatOpt = behandlingsresultatRepository.hentHvisEksisterer(behandlingId);
        if (behandlingsresultatOpt.isEmpty() || behandlingsresultatOpt.get().getVilkårResultat() == null) {
            return Optional.empty();
        }
        var behandlingsresultat = behandlingsresultatOpt.get();
        var medlemskapsvilkåret = behandlingsresultat.getVilkårResultat()
            .getVilkårene()
            .stream()
            .filter(vilkårType -> vilkårType.getVilkårType().equals(VilkårType.MEDLEMSKAPSVILKÅRET))
            .findFirst();

        if (medlemskapsvilkåret.isPresent()) {
            var medlem = medlemskapsvilkåret.get();
            if (medlem.getGjeldendeVilkårUtfall().equals(VilkårUtfallType.OPPFYLT)) {
                var behandling = behandlingRepository.hentBehandling(behandlingId);
                return medlemskapVilkårPeriodeRepository.hentOpphørsdatoHvisEksisterer(behandling);
            }
            if (medlem.getGjeldendeVilkårUtfall().equals(VilkårUtfallType.IKKE_OPPFYLT)) {
                return Optional.empty();
            }
        }
        return Optional.empty();
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
