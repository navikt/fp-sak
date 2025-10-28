package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "migrering.populeradopsjon", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class AdopsjonPopulerDelvikarTask extends BehandlingProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(AdopsjonPopulerDelvikarTask.class);

    private EntityManager entityManager;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private FamilieHendelseRepository familieHendelseRepository;

    public AdopsjonPopulerDelvikarTask() {
        // For CDI
    }

    @Inject
    public AdopsjonPopulerDelvikarTask(BehandlingRepositoryProvider repositoryProvider) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.entityManager = repositoryProvider.getEntityManager();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
    }



    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long behandlingId) {
        behandlingRepository.taSkriveLås(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        // - Populer AdopsjonEntitet med delvilkår
        var behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandlingId).orElseThrow();
        familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())
            .flatMap(FamilieHendelseGrunnlagEntitet::getGjeldendeBekreftetVersjon)
            .ifPresent(fh -> oppdaterAdopsjonEntitet(behandling, behandlingsresultat, fh));

        // - Oppdater vilkår til nytt
        var vilkårResultatId = behandlingsresultatRepository.hentHvisEksisterer(behandlingId)
            .map(Behandlingsresultat::getVilkårResultat)
            .map(VilkårResultat::getId).orElseThrow();
        entityManager.createNativeQuery("""
                UPDATE VILKAR
                SET vilkar_type = :omsvilkar
                WHERE vilkar_resultat_id = :vrid
                  and vilkar_type in ('FP_VK_16','FP_VK_4','FP_VK_5','FP_VK_8','FP_VK_33')
               """)
            .setParameter("omsvilkar", VilkårType.OMSORGSOVERTAKELSEVILKÅR.getKode())
            .setParameter("vrid", vilkårResultatId)
            .executeUpdate();
        entityManager.flush();
    }

    private void oppdaterAdopsjonEntitet(Behandling  behandling, Behandlingsresultat  behandlingsresultat, FamilieHendelseEntitet familieHendelse) {
        if (familieHendelse.getAdopsjon().isEmpty()) return;
        var adopsjon = familieHendelse.getAdopsjon().orElseThrow();
        var vilkårene = behandlingsresultat.getVilkårResultat().getVilkårene().stream().map(Vilkår::getVilkårType).toList();

        var brukDelvilkår = OmsorgsovertakelseVilkårType.UDEFINERT;
        if (FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType())) {
            if (vilkårene.stream().anyMatch(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD::equals)) {
                brukDelvilkår = OmsorgsovertakelseVilkårType.FP_FORELDREANSVARSVILKÅRET_2_LEDD;
            } else {
                brukDelvilkår = adopsjon.isStebarnsadopsjon() ? OmsorgsovertakelseVilkårType.FP_STEBARNSADOPSJONSVILKÅRET : OmsorgsovertakelseVilkårType.FP_ADOPSJONSVILKÅRET;
            }
        } else if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
            if (vilkårene.stream().anyMatch(VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD::equals)) {
                brukDelvilkår = OmsorgsovertakelseVilkårType.ES_ADOPSJONSVILKÅRET;
            } else if (vilkårene.stream().anyMatch(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD::equals)) {
                brukDelvilkår = OmsorgsovertakelseVilkårType.ES_FORELDREANSVARSVILKÅRET_2_LEDD;
            } else if (vilkårene.stream().anyMatch(VilkårType.OMSORGSVILKÅRET::equals)) {
                brukDelvilkår = OmsorgsovertakelseVilkårType.ES_OMSORGSVILKÅRET;
            } else if (vilkårene.stream().anyMatch(VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD::equals)) {
                brukDelvilkår = OmsorgsovertakelseVilkårType.ES_FORELDREANSVARSVILKÅRET_4_LEDD;
            }
        }
        if (OmsorgsovertakelseVilkårType.UDEFINERT.equals(brukDelvilkår)) {
            LOG.warn("AdopsjonPopuler: Kan ikke utlede delvilkår for sak {} behandling {} vilkår {}",
                behandling.getSaksnummer().getVerdi(), behandling.getId(), vilkårene);
            throw new IllegalStateException("Adopsjon: finner ikke relevant delvilkår");
        }
        if (!OmsorgsovertakelseVilkårType.UDEFINERT.equals(adopsjon.getOmsorgovertakelseVilkår())) {
            if (brukDelvilkår.equals(adopsjon.getOmsorgovertakelseVilkår())) {
                return;
            }
            LOG.warn("AdopsjonPopuler: Ulikt delvilkår for sak {} behandling {} eksisterende delvilkår {} utledet delvilkår {}",
                behandling.getSaksnummer().getVerdi(), behandling.getId(), adopsjon.getOmsorgovertakelseVilkår(), brukDelvilkår);
            if (!(OmsorgsovertakelseVilkårType.FP_FORELDREANSVARSVILKÅRET_2_LEDD.equals(brukDelvilkår)
                && OmsorgsovertakelseVilkårType.ES_FORELDREANSVARSVILKÅRET_2_LEDD.equals(adopsjon.getOmsorgovertakelseVilkår()))) {
                throw new IllegalStateException("Adopsjon: avvikende delvilkår");
            }
        }
        entityManager.createNativeQuery("""
                UPDATE FH_ADOPSJON
                SET omsorg_vilkaar_type = :brukvilkar
                WHERE id = :adid
               """)
            .setParameter("brukvilkar", brukDelvilkår.getKode())
            .setParameter("adid", adopsjon.getId())
            .executeUpdate();
        entityManager.flush();
    }
}
