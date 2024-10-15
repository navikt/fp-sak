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

import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapsvilkårVurderingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapsvilkårVurderingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
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
    private final MedlemskapsvilkårVurderingRepository medlemskapsvilkårVurderingRepository;
    private final VilkårResultatRepository vilkårResultatRepository;

    @Inject
    public MedlemskapMigreringTask(EntityManager entityManager,
                                   ProsessTaskTjeneste prosessTaskTjeneste,
                                   BehandlingRepository behandlingRepository,
                                   MedlemTjeneste medlemTjeneste,
                                   SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                   MedlemskapsvilkårVurderingRepository medlemskapsvilkårVurderingRepository,
                                   VilkårResultatRepository vilkårResultatRepository) {
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.medlemTjeneste = medlemTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.medlemskapsvilkårVurderingRepository = medlemskapsvilkårVurderingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
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
                    var medlemskapsvilkårVurderingEntitet = MedlemskapsvilkårVurderingEntitet.forMedlemFom(vilkårResultat, medlemFom.get());
                    //medlemskapsvilkårVurderingRepository.lagre(medlemskapsvilkårVurderingEntitet);
                }
            }
            case FORELDREPENGER, SVANGERSKAPSPENGER -> {
                var opphørsdato = finnOpphørsdato(behandlingId);
                if (opphørsdato.isPresent()) {
                    var opphørsårsak = medlemTjeneste.hentAvslagsårsak(behandlingId);
                    LOG.info("Opphør for {} {} {} {}", behandling.getFagsakYtelseType(), behandlingId, opphørsdato.get(), opphørsårsak.orElse(null));
                    var medlemskapsvilkårVurderingEntitet = MedlemskapsvilkårVurderingEntitet.forOpphør(vilkårResultat, opphørsdato.get(),
                        opphørsårsak.orElse(null));
                    //medlemskapsvilkårVurderingRepository.lagre(medlemskapsvilkårVurderingEntitet);
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + behandling.getFagsakYtelseType());
        }
    }

    private Optional<LocalDate> finnOpphørsdato(Long behandlingId) {
        var opphørsdato = medlemTjeneste.hentOpphørsdatoHvisEksisterer(behandlingId);
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
