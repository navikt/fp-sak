package no.nav.foreldrepenger.domene.risikoklassifisering;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.domene.risikoklassifisering.task.RisikoklassifiseringUtførTask;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.RisikovurderingTjeneste;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.kafka.AktoerIdDto;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.kafka.AnnenPart;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.kafka.Opplysningsperiode;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.kafka.RequestWrapper;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.kafka.RisikovurderingRequest;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
public class Risikoklassifisering {

    private static final Logger LOG = LoggerFactory.getLogger(Risikoklassifisering.class);
    private ProsessTaskRepository prosessTaskRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private RisikovurderingTjeneste risikovurderingTjeneste;
    private OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste;
    private PersonopplysningRepository personopplysningRepository;
    private FamilieHendelseRepository familieHendelseRepository;

    Risikoklassifisering() {
        // CDI proxy
    }

    @Inject
    public Risikoklassifisering(ProsessTaskRepository prosessTaskRepository,
                                SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                RisikovurderingTjeneste risikovurderingTjeneste,
                                OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste,
                                PersonopplysningRepository personopplysningRepository,
                                FamilieHendelseRepository familieHendelseRepository) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.risikovurderingTjeneste = risikovurderingTjeneste;
        this.opplysningsPeriodeTjeneste = opplysningsPeriodeTjeneste;
        this.personopplysningRepository = personopplysningRepository;
        this.familieHendelseRepository = familieHendelseRepository;
    }

    public void opprettProsesstaskForRisikovurdering(BehandlingReferanse ref) {
        try {
            var task = opprettPotensiellTaskProsesstask(ref);
            task.ifPresent(t -> prosessTaskRepository.lagre(t));
        } catch (Exception ex) {
            LOG.warn("Publisering av Risikovurderingstask feilet", ex);
        }
    }

    Optional<ProsessTaskData> opprettPotensiellTaskProsesstask(BehandlingReferanse ref) throws IOException {
        var behandlingId = ref.getBehandlingId();
        if (risikovurderingTjeneste.behandlingHarBlittRisikoklassifisert(behandlingId)) {
            LOG.info("behandling = {} Har Blitt Risikoklassifisert", behandlingId);
            return Optional.empty();
        }
        var risikovurderingRequest = opprettRequest(ref, behandlingId);
        return Optional.of(opprettTaskForRequest(ref, behandlingId, risikovurderingRequest));
    }

    private String hentBehandlingTema(BehandlingReferanse ref) {
        Optional<FamilieHendelseGrunnlagEntitet> grunnlag = familieHendelseRepository.hentAggregatHvisEksisterer(
            ref.getBehandlingId());
        BehandlingTema behandlingTema = BehandlingTema.fraFagsak(ref.getFagsakYtelseType(),
            grunnlag.map(FamilieHendelseGrunnlagEntitet::getSøknadVersjon).orElseThrow());
        return behandlingTema.getOffisiellKode();
    }

    private ProsessTaskData opprettTaskForRequest(BehandlingReferanse ref,
                                                  Long behandlingId,
                                                  RisikovurderingRequest risikovurderingRequest) throws IOException {
        ProsessTaskData taskData = new ProsessTaskData(RisikoklassifiseringUtførTask.TASKTYPE);
        taskData.setBehandling(ref.getFagsakId(), behandlingId, ref.getAktørId().getId());
        taskData.setCallIdFraEksisterende();
        RequestWrapper requestWrapper = new RequestWrapper(MDCOperations.getCallId(), risikovurderingRequest);
        taskData.setProperty(RisikoklassifiseringUtførTask.KONSUMENT_ID,
            risikovurderingRequest.getKonsumentId().toString());
        taskData.setProperty(RisikoklassifiseringUtførTask.RISIKOKLASSIFISERING_JSON, getJson(requestWrapper));
        return taskData;
    }

    private RisikovurderingRequest opprettRequest(BehandlingReferanse ref, Long behandlingId) {
        LocalDate skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId)
            .getUtledetSkjæringstidspunkt();
        Interval interval = opplysningsPeriodeTjeneste.beregn(behandlingId, ref.getFagsakYtelseType());
        return RisikovurderingRequest.builder()
            .medSoekerAktoerId(new AktoerIdDto(ref.getAktørId().getId()))
            .medBehandlingstema(hentBehandlingTema(ref))
            .medSkjæringstidspunkt(skjæringstidspunkt)
            .medOpplysningsperiode(leggTilOpplysningsperiode(interval))
            .medAnnenPart(leggTilAnnenPart(ref))
            .medKonsumentId(ref.getBehandlingUuid())
            .build();
    }

    private AnnenPart leggTilAnnenPart(BehandlingReferanse ref) {
        var oppgittAnnenPart = personopplysningRepository.hentPersonopplysningerHvisEksisterer(ref.getBehandlingId())
            .flatMap(PersonopplysningGrunnlagEntitet::getOppgittAnnenPart);
        if (oppgittAnnenPart.isPresent()) {
            var aktoerId =
                oppgittAnnenPart.get().getAktørId() == null ? null : oppgittAnnenPart.get().getAktørId().getId();
            if (aktoerId != null) {
                return new AnnenPart(new AktoerIdDto(aktoerId));
            }
            String utenlandskFnr = oppgittAnnenPart.get().getUtenlandskPersonident();
            if (utenlandskFnr != null) {
                return new AnnenPart(utenlandskFnr);
            }
        }
        return null;
    }

    private Opplysningsperiode leggTilOpplysningsperiode(Interval interval) {
        LocalDate tilOgMed =
            interval.getEnd() == null ? null : LocalDate.ofInstant(interval.getEnd(), ZoneId.systemDefault());
        return new Opplysningsperiode(LocalDate.ofInstant(interval.getStart(), ZoneId.systemDefault()), tilOgMed);
    }

    private String getJson(RequestWrapper risikovurderingRequest) throws IOException {
        return JsonObjectMapper.getJson(risikovurderingRequest);
    }
}
