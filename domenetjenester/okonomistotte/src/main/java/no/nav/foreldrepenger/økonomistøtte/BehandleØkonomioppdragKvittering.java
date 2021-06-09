package no.nav.foreldrepenger.økonomistøtte;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.OppdragKvittering;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHendelse;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHendelseMottak;

@ApplicationScoped
@ActivateRequestContext
@Transactional
public class BehandleØkonomioppdragKvittering {

    private ProsessTaskHendelseMottak hendelsesmottak;
    private ØkonomioppdragRepository økonomioppdragRepository;
    private BehandleNegativeKvitteringTjeneste behandleNegativeKvittering;

    private static final Logger LOG = LoggerFactory.getLogger(BehandleØkonomioppdragKvittering.class);

    BehandleØkonomioppdragKvittering() {
        // for CDI
    }

    @Inject
    public BehandleØkonomioppdragKvittering(ProsessTaskHendelseMottak hendelsesmottak,
                                            ØkonomioppdragRepository økonomioppdragRepository,
                                            BehandleNegativeKvitteringTjeneste behandleNegativeKvitteringTjeneste) {
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
        var behandlingId = kvittering.getBehandlingId();
        LOG.info("Behandler økonomikvittering med resultatkode: {} i behandling: {}", kvittering.getAlvorlighetsgrad(), behandlingId);

        LOG.info("Letter etter oppdrag uten kvittering for behandling {} og fagsystemId {}.", behandlingId, kvittering.getFagsystemId());
        var oppdragUtenKvittering = økonomioppdragRepository.hentOppdragUtenKvittering(kvittering.getFagsystemId(), behandlingId);
        LOG.debug("Oppdrag uten kvittering: {}", oppdragUtenKvittering);

        var oppdragKvittering = OppdragKvittering.builder()
            .medAlvorlighetsgrad(kvittering.getAlvorlighetsgrad())
            .medMeldingKode(kvittering.getMeldingKode())
            .medBeskrMelding(kvittering.getBeskrMelding())
            .medOppdrag110(oppdragUtenKvittering)
            .build();

        økonomioppdragRepository.lagre(oppdragKvittering);

        var oppdragskontroll = oppdragUtenKvittering.getOppdragskontroll();
        LOG.debug("Fant oppdragskontroll: {}", oppdragskontroll);

        var erAlleKvitteringerMottatt = sjekkAlleKvitteringMottatt(oppdragskontroll.getOppdrag110Liste());

        if (erAlleKvitteringerMottatt) {
            LOG.info("Alle økonomioppdrag-kvitteringer er mottatt for behandling: {}", behandlingId);
            oppdragskontroll.setVenterKvittering(false);

            if (oppdaterProsesstask) {
                //Dersom kvittering viser positivt resultat: La Behandlingskontroll/TaskManager fortsette behandlingen - trigger prosesstask Behandling.Avslutte hvis brev er bekreftet levert
                var alleViserPositivtResultat = erAlleKvitteringerMedPositivtResultat(oppdragskontroll.getOppdrag110Liste());
                if (alleViserPositivtResultat) {
                    LOG.info("Alle økonomioppdrag-kvitteringer viser positivt resultat for behandling: {}", behandlingId);
                    hendelsesmottak.mottaHendelse(oppdragskontroll.getProsessTaskId(), ProsessTaskHendelse.ØKONOMI_OPPDRAG_KVITTERING);
                } else {
                    LOG.warn("Ikke alle økonomioppdrag-kvitteringer viser positivt resultat for behandling: {}", behandlingId);
                    behandleNegativeKvittering.nullstilleØkonomioppdragTask(oppdragskontroll.getProsessTaskId());
                }
            } else {
                LOG.info("Oppdaterer ikke prosesstask.");
            }
            økonomioppdragRepository.lagre(oppdragskontroll);
        }
    }

    private boolean sjekkAlleKvitteringMottatt(List<Oppdrag110> oppdrag110Liste) {
        if (oppdrag110Liste.isEmpty()) {
            throw new IllegalStateException("Det forventes at oppdrag110 finnes.");
        }
        return oppdrag110Liste.stream().noneMatch(Oppdrag110::venterKvittering);
    }

    private boolean erAlleKvitteringerMedPositivtResultat(List<Oppdrag110> oppdrag110Liste) {
        if (oppdrag110Liste.isEmpty()) {
            throw new IllegalStateException("Det forventes at oppdrag110 finnes.");
        }
        var grupperteOppdrag = oppdrag110Liste.stream().collect(Collectors.groupingBy(Oppdrag110::getFagsystemId));
        for (Map.Entry<Long, List<Oppdrag110>> entry : grupperteOppdrag.entrySet()) {
            var sisteOppdrag = entry.getValue().stream().max(Comparator.comparing(Oppdrag110::getOpprettetTidspunkt)).orElseThrow();
            if (!OppdragKvitteringTjeneste.harPositivKvittering(sisteOppdrag)) {
                return false;
            }
        }
        return true;
    }
}
