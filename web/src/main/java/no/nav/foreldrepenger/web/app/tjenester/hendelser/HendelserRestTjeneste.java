package no.nav.foreldrepenger.web.app.tjenester.hendelser;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.HendelseSorteringRepository;
import no.nav.foreldrepenger.behandlingslager.hendelser.HendelsemottakRepository;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.AktørIdDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.Endringstype;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.HendelseDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.HendelseWrapperDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.pdl.DødHendelseDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.pdl.DødfødselHendelseDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.pdl.FødselHendelseDto;
import no.nav.foreldrepenger.mottak.hendelser.JsonMapper;
import no.nav.foreldrepenger.mottak.hendelser.KlargjørHendelseTask;
import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt;

@Path("/hendelser")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class HendelserRestTjeneste {

    private static final Logger LOGGER = LoggerFactory.getLogger(HendelserRestTjeneste.class);

    private HendelseSorteringRepository sorteringRepository;
    private HendelsemottakRepository hendelsemottakRepository;
    private ProsessTaskRepository prosessTaskRepository;

    public HendelserRestTjeneste() {// For Rest-CDI
    }

    @Inject
    public HendelserRestTjeneste(HendelseSorteringRepository sorteringRepository,
                                 HendelsemottakRepository hendelsemottakRepository,
                                 ProsessTaskRepository prosessTaskRepository) {
        this.sorteringRepository = sorteringRepository;
        this.hendelsemottakRepository = hendelsemottakRepository;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    @POST
    @Path("/ping")
    @Operation(description = "Ping", tags = "hendelser")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, ressurs = BeskyttetRessursResourceAttributt.DRIFT)
    public EnkelRespons ping() {
        return new EnkelRespons("pong");
    }

    @Deprecated
    @POST
    @Path("/hendelse")
    @Operation(description = "Mottak av hendelser", tags = "hendelser")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, ressurs = BeskyttetRessursResourceAttributt.DRIFT)
    public EnkelRespons mottaHendelseV1(@Parameter(description = "Hendelse fra TPS eller Infotrygd") @Valid AbacHendelseWrapperV1Dto wrapperDto) {
        no.nav.foreldrepenger.kontrakter.abonnent.HendelseDto hendelseDto = wrapperDto.getHendelse();
        var beskrivelse = String.format("Hendelse mottatt fra %s av typen %s med id/sekvensnummer: %s.",
            hendelseDto.getAvsenderSystem(), hendelseDto.getHendelsetype(), hendelseDto.getId());
        LOGGER.info(beskrivelse);
        if (!hendelsemottakRepository.hendelseErNy(hendelseDto.getId())) {
            return new EnkelRespons("Hendelse ble ignorert. Hendelse med samme ID er allerede registrert");
        }
        return registrerHendelseV1(hendelseDto, beskrivelse);
    }

    @POST
    @Path("/motta")
    @Operation(description = "Mottak av hendelser", tags = "hendelser")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, ressurs = BeskyttetRessursResourceAttributt.DRIFT)
    public EnkelRespons mottaHendelse(@Parameter(description = "Hendelse fra Fpabonnent") @Valid AbacHendelseWrapperDto wrapperDto) {
        HendelseDto hendelseDto = wrapperDto.getHendelse();
        var beskrivelse = String.format("Hendelse mottatt fra %s av typen %s med hendelseId: %s.",
            hendelseDto.getAvsenderSystem(), hendelseDto.getHendelsetype(), hendelseDto.getId());
        LOGGER.info(beskrivelse);
        if (!hendelsemottakRepository.hendelseErNy(hendelseDto.getId())) {
            return new EnkelRespons("Hendelse ble ignorert. Hendelse med samme ID er allerede registrert.");
        }
        return registrerHendelse(hendelseDto, beskrivelse);
    }

    @POST
    @Operation(description = "Grovsortering av aktørID-er. Returnerer aktørID-er i listen som har en sak.", tags = "hendelser")
    @Path("/grovsorter")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, ressurs = BeskyttetRessursResourceAttributt.DRIFT)
    public List<String> grovSorter(@Parameter(description = "Liste med aktør IDer som skal sorteres") @Valid List<AbacAktørIdDto> aktoerIdListe) {
        List<AktørId> aktørIdList = aktoerIdListe.stream().map(AbacAktørIdDto::getAktørId).map(AktørId::new).collect(Collectors.toList()); // NOSONAR
        return sorteringRepository.hentEksisterendeAktørIderMedSak(aktørIdList).stream().map(AktørId::getId).collect(Collectors.toList());
    }

    private EnkelRespons registrerHendelseV1(no.nav.foreldrepenger.kontrakter.abonnent.HendelseDto hendelse, String beskrivelse) {
        var hendelsType = ForretningshendelseType.fraKode(hendelse.getHendelsetype());
        if (ForretningshendelseType.FØDSEL.equals(hendelsType)) {
            no.nav.foreldrepenger.kontrakter.abonnent.tps.FødselHendelseDto fødselV1 = (no.nav.foreldrepenger.kontrakter.abonnent.tps.FødselHendelseDto)hendelse;
            FødselHendelseDto fødsel = new FødselHendelseDto();
            fødsel.setId(fødselV1.getId());
            fødsel.setEndringstype(Endringstype.OPPRETTET);
            fødsel.setAktørIdForeldre(fødselV1.getAktørIdForeldre().stream().map(AktørIdDto::new).collect(Collectors.toList()));
            fødsel.setFødselsdato(fødselV1.getFødselsdato());
            return registrerHendelse(fødsel, beskrivelse);
        } else if (ForretningshendelseType.DØD.equals(hendelsType)) {
            no.nav.foreldrepenger.kontrakter.abonnent.tps.DødHendelseDto dødV1 = (no.nav.foreldrepenger.kontrakter.abonnent.tps.DødHendelseDto)hendelse;
            DødHendelseDto død = new DødHendelseDto();
            død.setId(dødV1.getId());
            død.setEndringstype(Endringstype.OPPRETTET);
            død.setAktørId(dødV1.getAktørId().stream().map(AktørIdDto::new).collect(Collectors.toList()));
            død.setDødsdato(dødV1.getDødsdato());
            return registrerHendelse(død, beskrivelse);
        } else if (ForretningshendelseType.DØDFØDSEL.equals(hendelsType)) {
            no.nav.foreldrepenger.kontrakter.abonnent.tps.DødfødselHendelseDto dødfødselV1 = (no.nav.foreldrepenger.kontrakter.abonnent.tps.DødfødselHendelseDto)hendelse;
            DødfødselHendelseDto dødfødsel = new DødfødselHendelseDto();
            dødfødsel.setId(dødfødselV1.getId());
            dødfødsel.setEndringstype(Endringstype.OPPRETTET);
            dødfødsel.setAktørId(dødfødselV1.getAktørId().stream().map(AktørIdDto::new).collect(Collectors.toList()));
            dødfødsel.setDødfødselsdato(dødfødselV1.getDødfødselsdato());
            return registrerHendelse(dødfødsel, beskrivelse);
        } else {
            LOGGER.error("Kan ikke håndtere {}", beskrivelse);
            return new EnkelRespons("Ukjent hendelse");
        }
    }

    private EnkelRespons registrerHendelse(HendelseDto hendelse, String beskrivelse) {
        var hendelsType = ForretningshendelseType.fraKode(hendelse.getHendelsetype());
        if (ForretningshendelseType.UDEFINERT.equals(hendelsType)) {
            LOGGER.warn("Kan ikke håndtere {}", beskrivelse);
            return new EnkelRespons("Ukjent hendelse");
        }

        hendelsemottakRepository.registrerMottattHendelse(hendelse.getId());
        ProsessTaskData taskData = new ProsessTaskData(KlargjørHendelseTask.TASKTYPE);
        taskData.setPayload(JsonMapper.toJson(hendelse));
        taskData.setProperty(KlargjørHendelseTask.PROPERTY_HENDELSE_TYPE, hendelse.getHendelsetype());
        taskData.setProperty(KlargjørHendelseTask.PROPERTY_UID, hendelse.getId());
        taskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(taskData);
        return new EnkelRespons("OK");
    }

    public static class AbacAktørIdDto extends AktørIdDto implements AbacDto {
        public AbacAktørIdDto() {
        }

        public AbacAktørIdDto(String aktørId) {
            super(aktørId);
        }

        @Override
        public AbacDataAttributter abacAttributter() {
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.AKTØR_ID, this.getAktørId());
        }
    }

    public static class AbacHendelseWrapperV1Dto extends no.nav.foreldrepenger.kontrakter.abonnent.HendelseWrapperDto implements AbacDto {
        public AbacHendelseWrapperV1Dto() {
        }

        public AbacHendelseWrapperV1Dto(no.nav.foreldrepenger.kontrakter.abonnent.HendelseDto hendelse) {
            super(hendelse);
        }

        @Override
        public AbacDataAttributter abacAttributter() {
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.AKTØR_ID, new HashSet<>(this.getAlleAktørId()));
        }
    }

    public static class AbacHendelseWrapperDto extends HendelseWrapperDto implements AbacDto {
        public AbacHendelseWrapperDto() {
        }

        public AbacHendelseWrapperDto(HendelseDto hendelse) {
            super(hendelse);
        }

        @Override
        public AbacDataAttributter abacAttributter() {
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.AKTØR_ID, new HashSet<>(this.getAlleAktørId()));
        }
    }
}
