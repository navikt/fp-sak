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

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
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

    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    private ProsessTaskRepository prosessTaskRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private RisikovurderingTjeneste risikovurderingTjeneste;
    private OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste;
    private PersonopplysningRepository personopplysningRepository;
    private FamilieHendelseRepository familieHendelseRepository;

    Risikoklassifisering(){
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

    public void opprettProsesstaskForRisikovurdering(Behandling behandling) {
        try {
            LocalDate skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()).getUtledetSkjæringstidspunkt();
            Interval interval = opplysningsPeriodeTjeneste.beregn(behandling.getId(),behandling.getFagsakYtelseType());
            RisikovurderingRequest risikovurderingRequest = RisikovurderingRequest.builder()
                .medSoekerAktoerId(new AktoerIdDto(behandling.getAktørId().getId()))
                .medBehandlingstema(hentBehandlingTema(behandling))
                .medSkjæringstidspunkt(skjæringstidspunkt)
                .medOpplysningsperiode(leggTilOpplysningsperiode(interval))
                .medAnnenPart(leggTilAnnenPart(behandling))
                .medKonsumentId(behandling.getUuid()).build();
            opprettProsesstask(behandling, risikovurderingRequest);
        } catch (Exception ex) {
            log.warn("Publisering av Risikovurderingstask feilet", ex);
        }
    }

    private String hentBehandlingTema(Behandling behandling) {
        Optional<FamilieHendelseGrunnlagEntitet> grunnlag = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId());
        BehandlingTema behandlingTema = BehandlingTema.fraFagsak(behandling.getFagsak(),
            grunnlag.map(FamilieHendelseGrunnlagEntitet::getSøknadVersjon).get());
        return behandlingTema.getOffisiellKode();
    }

    private void opprettProsesstask(Behandling behandling, RisikovurderingRequest risikovurderingRequest) throws IOException {
        if (!risikovurderingTjeneste.behandlingHarBlittRisikoklassifisert(behandling.getId())) {
            ProsessTaskData taskData = new ProsessTaskData(RisikoklassifiseringUtførTask.TASKTYPE);
            taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
            taskData.setCallIdFraEksisterende();
            RequestWrapper requestWrapper = new RequestWrapper(MDCOperations.getCallId(), risikovurderingRequest);
            taskData.setProperty(RisikoklassifiseringUtførTask.KONSUMENT_ID, risikovurderingRequest.getKonsumentId().toString());
            taskData.setProperty(RisikoklassifiseringUtførTask.RISIKOKLASSIFISERING_JSON,getJson(requestWrapper));
            prosessTaskRepository.lagre(taskData);
        } else {
            log.info("behandling = {} Har Blitt Risikoklassifisert", behandling.getId());
        }
    }

    private AnnenPart leggTilAnnenPart(Behandling behandling) {
        Optional<OppgittAnnenPartEntitet> oppgittAnnenPart =
            personopplysningRepository.hentPersonopplysningerHvisEksisterer(behandling.getId())
                .flatMap(PersonopplysningGrunnlagEntitet::getOppgittAnnenPart);
        if(oppgittAnnenPart.isPresent()) {
            String aktoerId = oppgittAnnenPart.get().getAktørId() == null ? null :
                oppgittAnnenPart.get().getAktørId().getId();
            if (aktoerId != null) {
                return new AnnenPart(new AktoerIdDto(aktoerId));
            }
            String utenlandskFnr = oppgittAnnenPart.get().getUtenlandskPersonident();
            if(utenlandskFnr != null) {
                return new AnnenPart(utenlandskFnr);
            }
        }
        return null;
    }

    private Opplysningsperiode leggTilOpplysningsperiode(Interval interval) {
        LocalDate tilOgMed = interval.getEnd() == null ? null : LocalDate.ofInstant(interval.getEnd(),ZoneId.systemDefault());
        return new Opplysningsperiode(LocalDate.ofInstant(interval.getStart(), ZoneId.systemDefault()), tilOgMed);
    }

    private String getJson(RequestWrapper risikovurderingRequest) throws IOException {
        return JsonObjectMapper.getJson(risikovurderingRequest);
    }
}
