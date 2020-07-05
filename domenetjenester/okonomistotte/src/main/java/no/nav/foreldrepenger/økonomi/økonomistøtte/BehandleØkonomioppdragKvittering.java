package no.nav.foreldrepenger.økonomi.økonomistøtte;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.OppdragKvittering;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHendelse;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHendelseMottak;

@ApplicationScoped
@ActivateRequestContext
@Transactional
public class BehandleØkonomioppdragKvittering {

    private AlleMottakereHarPositivKvitteringProvider alleMottakereHarPositivKvitteringProvider;
    private ProsessTaskHendelseMottak hendelsesmottak;
    private ØkonomioppdragRepository økonomioppdragRepository;
    private BehandleNegativeKvitteringTjeneste behandleNegativeKvittering;

    private static final Logger log = LoggerFactory.getLogger(BehandleØkonomioppdragKvittering.class);

    BehandleØkonomioppdragKvittering() {
        // for CDI
    }

    @Inject
    public BehandleØkonomioppdragKvittering(AlleMottakereHarPositivKvitteringProvider alleMottakereHarPositivKvitteringProvider,
                                            ProsessTaskHendelseMottak hendelsesmottak,
                                            ØkonomioppdragRepository økonomioppdragRepository,
                                            BehandleNegativeKvitteringTjeneste behandleNegativeKvitteringTjeneste) {
        this.alleMottakereHarPositivKvitteringProvider = alleMottakereHarPositivKvitteringProvider;
        this.hendelsesmottak = hendelsesmottak;
        this.økonomioppdragRepository = økonomioppdragRepository;
        this.behandleNegativeKvittering = behandleNegativeKvitteringTjeneste;
    }

    /**
     * Finn tilsvarende oppdrag som venter på kvittering
     * og behandle det
     *
     * @param kvittering Kvittering fra oppdragssystemet
     */
    public void behandleKvittering(ØkonomiKvittering kvittering) {
        behandleKvittering(kvittering, true);
    }

    public void behandleKvittering(ØkonomiKvittering kvittering, boolean oppdaterProsesstask) {
        Long behandlingId = kvittering.getBehandlingId();

        log.info("Behandler økonomikvittering med resultatkode: {} i behandling: {}", kvittering.getAlvorlighetsgrad(), behandlingId); //$NON-NLS-1$
        //Korrelere med lagret oppdrag
        Oppdragskontroll oppdrag = økonomioppdragRepository.finnVentendeOppdrag(behandlingId);

        // oppdatere status i Økonomioppdrag-datalager
        List<Oppdrag110> okoOppdrag110Liste = oppdrag.getOppdrag110Liste();

        List<Oppdrag110> okoOppdrag110UtenKvitteringListe = okoOppdrag110Liste.stream()
            .filter(Oppdrag110::venterKvittering)
            .filter(oppdr110 -> oppdr110.getFagsystemId() == kvittering.getFagsystemId())
            .collect(Collectors.toList());

        if (okoOppdrag110UtenKvitteringListe.isEmpty()) {
            throw new IllegalStateException("Finnes ikke oppdrag for kvittering med fagsystemId: " + kvittering.getFagsystemId());
        } else if (okoOppdrag110UtenKvitteringListe.size() > 1) {
            throw new IllegalStateException("Finnes flere oppdrag uten kvittering med samme fagsystemId: " + kvittering.getFagsystemId());
        }

        Oppdrag110 okoOppdrag110 = okoOppdrag110UtenKvitteringListe.get(0);

        OppdragKvittering.builder()
            .medAlvorlighetsgrad(kvittering.getAlvorlighetsgrad())
            .medMeldingKode(kvittering.getMeldingKode())
            .medBeskrMelding(kvittering.getBeskrMelding())
            .medOppdrag110(okoOppdrag110)
            .build();

        boolean erAlleKvitteringerMottatt = sjekkAlleKvitteringMottatt(okoOppdrag110Liste);

        if (erAlleKvitteringerMottatt) {
            log.info("Alle økonomioppdrag-kvitteringer er mottatt for behandling: {}", behandlingId);
            oppdrag.setVenterKvittering(false);

            if (oppdaterProsesstask) {
                //Dersom kvittering viser positivt resultat: La Behandlingskontroll/TaskManager fortsette behandlingen - trigger prosesstask Behandling.Avslutte hvis brev er bekreftet levert
                boolean alleViserPositivtResultat = erAlleKvitteringerMedPositivtResultat(behandlingId, oppdrag);
                if (alleViserPositivtResultat) {
                    log.info("Alle økonomioppdrag-kvitteringer viser positivt resultat for behandling: {}", behandlingId);
                    hendelsesmottak.mottaHendelse(oppdrag.getProsessTaskId(), ProsessTaskHendelse.ØKONOMI_OPPDRAG_KVITTERING);
                } else {
                    log.warn("Ikke alle økonomioppdrag-kvitteringer viser positivt resultat for behandling: {}", behandlingId);
                    behandleNegativeKvittering.nullstilleØkonomioppdragTask(oppdrag.getProsessTaskId());
                }
            } else {
                log.info("Oppdaterer ikke prosesstask");
            }
        }
        økonomioppdragRepository.lagre(oppdrag);
    }

    private boolean sjekkAlleKvitteringMottatt(List<Oppdrag110> oppdrag110Liste) {
        return oppdrag110Liste.stream().noneMatch(Oppdrag110::venterKvittering);
    }

    private boolean erAlleKvitteringerMedPositivtResultat(Long behandlingId, Oppdragskontroll oppdrag) {
        AlleMottakereHarPositivKvittering tjeneste = alleMottakereHarPositivKvitteringProvider.getTjeneste(behandlingId);
        return tjeneste.vurder(oppdrag);
    }

}
