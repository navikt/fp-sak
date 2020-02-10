package no.nav.foreldrepenger.web.app.tjenester.hendelser;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
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
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.kontrakter.abonnent.AktørIdDto;
import no.nav.foreldrepenger.kontrakter.abonnent.HendelseDto;
import no.nav.foreldrepenger.kontrakter.abonnent.HendelseWrapperDto;
import no.nav.foreldrepenger.mottak.hendelser.HendelseSorteringTjeneste;
import no.nav.foreldrepenger.mottak.hendelser.MottattHendelseTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.hendelser.impl.ForretningshendelseRegistrererProvider;
import no.nav.vedtak.felles.jpa.Transaction;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt;

@Path("/hendelser")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transaction
public class HendelserRestTjeneste {

    private static final Logger LOGGER = LoggerFactory.getLogger(HendelserRestTjeneste.class);

    private MottattHendelseTjeneste mottattHendelseTjeneste;
    private HendelseSorteringTjeneste hendelseSorteringTjeneste;
    private ForretningshendelseRegistrererProvider forretningshendelseRegistrererProvider;

    public HendelserRestTjeneste() {// For Rest-CDI
    }

    @Inject
    public HendelserRestTjeneste(MottattHendelseTjeneste mottattHendelseTjeneste,
                                 HendelseSorteringTjeneste hendelseSorteringTjeneste,
                                 ForretningshendelseRegistrererProvider forretningshendelseRegistrererProvider) {
        this.mottattHendelseTjeneste = mottattHendelseTjeneste;
        this.hendelseSorteringTjeneste = hendelseSorteringTjeneste;
        this.forretningshendelseRegistrererProvider = forretningshendelseRegistrererProvider;
    }

    @POST
    @Path("/ping")
    @Operation(description = "Ping", tags = "hendelser")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, ressurs = BeskyttetRessursResourceAttributt.DRIFT)
    public EnkelRespons ping() {
        return new EnkelRespons("pong");
    }

    @POST
    @Path("/hendelse")
    @Operation(description = "Mottak av hendelser", tags = "hendelser")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, ressurs = BeskyttetRessursResourceAttributt.DRIFT)
    public EnkelRespons mottaHendelse(@Parameter(description = "Hendelse fra TPS eller Infotrygd") @Valid AbacHendelseWrapperDto wrapperDto) {
        HendelseDto hendelseDto = wrapperDto.getHendelse();
        loggTypeHendelse(hendelseDto.getAvsenderSystem(), hendelseDto.getHendelsetype(), hendelseDto.getId());
        if (!mottattHendelseTjeneste.erHendelseNy(hendelseDto.getId())) {
            return new EnkelRespons("Hendelse ble ignorert. Hendelse med samme ID er allerede registrert");
        }
        return registrerHendelse(hendelseDto);
    }

    @POST
    @Operation(description = "Grovsortering av aktørID-er. Returnerer aktørID-er i listen som har en sak.", tags = "hendelser")
    @Path("/grovsorter")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, ressurs = BeskyttetRessursResourceAttributt.DRIFT)
    public List<String> grovSorter(@Parameter(description = "Liste med aktør IDer som skal sorteres") @Valid List<AbacAktørIdDto> aktoerIdListe) {
        List<AktørId> aktørIdList = aktoerIdListe.stream().map(AbacAktørIdDto::getAktørId).map(AktørId::new).collect(Collectors.toList()); // NOSONAR
        return hendelseSorteringTjeneste.hentAktørIderTilknyttetSak(aktørIdList).stream().map(AktørId::getId).collect(Collectors.toList());
    }

    private EnkelRespons registrerHendelse(HendelseDto hendelseDto) {
        ForretningshendelseRegistrerer<HendelseDto> registrerer;
        try {
            registrerer = forretningshendelseRegistrererProvider.finnRegistrerer(hendelseDto);
        } catch (IllegalArgumentException iae) {
            LOGGER.info("Kan ikke håndtere hendelse fra {} av typen {} med id/sekvensnummer: {}.",
                hendelseDto.getAvsenderSystem(), hendelseDto.getHendelsetype(), hendelseDto.getId(), iae);// NOSONAR //$NON-NLS-1$
            return new EnkelRespons("Ukjent hendelse");
        }
        return registrerer.registrer(hendelseDto);
    }

    private static void loggTypeHendelse(String hendelseFra, String hendelseType, String uuid) {
        LOGGER.info("Hendelse mottatt fra {} av typen {} med id/sekvensnummer: {}.", hendelseFra, hendelseType, uuid);// NOSONAR //$NON-NLS-1$
    }

    public static class AbacAktørIdDto extends AktørIdDto implements AbacDto {
        public AbacAktørIdDto() {
        }

        public AbacAktørIdDto(String aktørId) {
            super(aktørId);
        }

        @Override
        public AbacDataAttributter abacAttributter() {
            return AbacDataAttributter.opprett();
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
            return AbacDataAttributter.opprett();
        }
    }
}
