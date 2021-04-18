package no.nav.foreldrepenger.domene.registerinnhenting.task;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.domene.registerinnhenting.RegisterdataInnhenter;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;

@ApplicationScoped
@ProsessTask(InnhentIAYIAbakusTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class InnhentIAYIAbakusTask extends GenerellProsessTask {

    public static final String TASKTYPE = "innhentsaksopplysninger.abakus";
    public static final String OVERSTYR_KEY = "overstyrt";
    public static final String OVERSTYR_VALUE = "overstyrt";
    public static final String IAY_REGISTERDATA_CALLBACK = "IAY_REGISTERDATA_CALLBACK";
    public static final String OPPDATERT_GRUNNLAG_KEY = "oppdagertGrunnlag";

    private static final Logger LOG = LoggerFactory.getLogger(InnhentIAYIAbakusTask.class);

    private BehandlingRepository behandlingRepository;
    private ProsessTaskRepository prosessTaskRepository;
    private RegisterdataInnhenter registerdataInnhenter;

    InnhentIAYIAbakusTask() {
        // for CDI proxy
    }

    @Inject
    public InnhentIAYIAbakusTask(BehandlingRepository behandlingRepository,
                                 ProsessTaskRepository prosessTaskRepository,
                                 RegisterdataInnhenter registerdataInnhenter) {
        super();
        this.behandlingRepository = behandlingRepository;
        this.prosessTaskRepository = prosessTaskRepository;
        this.registerdataInnhenter = registerdataInnhenter;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {

        var hendelse = Optional.ofNullable(prosessTaskData.getPropertyValue(ProsessTaskData.HENDELSE_PROPERTY));
        var grunnlag = Optional.ofNullable(prosessTaskData.getPropertyValue(OPPDATERT_GRUNNLAG_KEY));
        if (hendelse.isPresent() && grunnlag.filter(s -> !s.isEmpty()).isPresent()) {
            validerHendelse(prosessTaskData);
            return;
        }

        if (hendelse.isEmpty() || grunnlag.isEmpty()) {
            var overstyr = prosessTaskData.getPropertyValue(OVERSTYR_KEY) != null && OVERSTYR_VALUE.equals(prosessTaskData.getPropertyValue(OVERSTYR_KEY));
            var behandling = behandlingRepository.hentBehandling(behandlingId);

            precondition(behandling);

            LOG.info("Innhenter IAY-opplysninger i abakus for behandling: {}", behandling.getId());
            if (overstyr) {
                registerdataInnhenter.innhentFullIAYIAbakus(behandling);
                return;
            }
            registerdataInnhenter.innhentIAYIAbakus(behandling);
        }
        settTaskPåVent(prosessTaskData);
    }

    private void precondition(Behandling behandling) {
        if (behandling.erSaksbehandlingAvsluttet()) {
            throw new IllegalStateException("Utvikler-feil - saken er ferdig behandlet, kan ikke oppdateres. behandlingId=" + behandling.getId()
                + ", behandlingStatus=" + behandling.getStatus()
                + ", startpunkt=" + behandling.getStartpunkt());
        } else {
            LOG.info("Innhenter IAY-opplysninger i abakus for behandling: {}", behandling.getId());
        }
    }

    private void validerHendelse(ProsessTaskData prosessTaskData) {

        Optional.ofNullable(prosessTaskData.getPropertyValue(ProsessTaskData.HENDELSE_PROPERTY))
            .filter(IAY_REGISTERDATA_CALLBACK::equals).orElseThrow(() -> new IllegalStateException("Ugyldig hendelse"));
        LOG.info("Nytt aktivt grunnlag for behandling={} i abakus har uuid={}", prosessTaskData.getBehandlingId(), prosessTaskData.getPropertyValue(OPPDATERT_GRUNNLAG_KEY));
    }

    private void settTaskPåVent(ProsessTaskData prosessTaskData) {
        prosessTaskData.setProperty(ProsessTaskData.HENDELSE_PROPERTY, IAY_REGISTERDATA_CALLBACK);
        prosessTaskData.setStatus(ProsessTaskStatus.VENTER_SVAR);
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }
}
