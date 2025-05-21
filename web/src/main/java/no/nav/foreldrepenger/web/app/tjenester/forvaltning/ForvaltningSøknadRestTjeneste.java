package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.SaksnummerAnnenpartIdentDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.SaksnummerFødselsdatoDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.SaksnummerTermindatoDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/forvaltningSoknad")
@ApplicationScoped
@Transactional
public class ForvaltningSøknadRestTjeneste {

    private FamilieHendelseRepository familieHendelseRepository;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private PersonopplysningRepository personopplysningRepository;
    private EntityManager entityManager;
    private PersoninfoAdapter personinfoAdapter;
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    @Inject
    public ForvaltningSøknadRestTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                         FamilieHendelseTjeneste familieHendelseTjeneste,
                                         PersoninfoAdapter personinfoAdapter) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        this.entityManager = repositoryProvider.getEntityManager();
        this.personinfoAdapter = personinfoAdapter;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
    }

    public ForvaltningSøknadRestTjeneste() {
        // CDI
    }

    @POST
    @Path("/endreTermindato")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(description = "Oppdater termindato åpen/siste behandling ifm prematur situasjons", tags = "FORVALTNING-søknad")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response endreTermindato(@BeanParam @Valid SaksnummerTermindatoDto dto) {
        var fagsakId = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(dto.getSaksnummer())).map(Fagsak::getId).orElseThrow();
        var behandling = behandlingRepository.hentÅpneYtelseBehandlingerForFagsakIdForUpdate(fagsakId).stream()
                .filter(SpesialBehandling::erIkkeSpesialBehandling)
                .findFirst().orElseGet(() -> behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId).orElseThrow());
        var antall = familieHendelseRepository.oppdaterGjeldendeTermindatoForBehandling(behandling.getId(), dto.getTermindato(),
                dto.getUtstedtdato(), dto.getBegrunnelse());

        return Response.ok(antall).build();
    }

    @POST
    @Path("/manglendeTermindato")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(description = "Legg til terminbekreftelse på åpen/siste behandling ifm prematur situasjons", tags = "FORVALTNING-søknad")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response manglendeTermindato(@BeanParam @Valid SaksnummerTermindatoDto dto) {
        var fagsakId = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(dto.getSaksnummer())).map(Fagsak::getId).orElseThrow();
        var behandling = behandlingRepository.hentÅpneYtelseBehandlingerForFagsakIdForUpdate(fagsakId).stream()
            .filter(SpesialBehandling::erIkkeSpesialBehandling)
            .findFirst().orElseGet(() -> behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId).orElseThrow());
        var behandlingId = behandling.getId();
        var gjeldende = familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId);
        if (gjeldende.isEmpty() || gjeldende.flatMap(FamilieHendelseGrunnlagEntitet::getGjeldendeTerminbekreftelse).isPresent())
            return Response.status(Response.Status.BAD_REQUEST).build();
        var hendelsebuilder = familieHendelseRepository.opprettBuilderForOverstyring(behandlingId);
        var terminbuilder = hendelsebuilder.getTerminbekreftelseBuilder()
            .medTermindato(dto.getTermindato())
            .medUtstedtDato(dto.getUtstedtdato())
            .medNavnPå(dto.getBegrunnelse());
        familieHendelseRepository.lagreOverstyrtHendelse(behandlingId, hendelsebuilder.medTerminbekreftelse(terminbuilder));

        return Response.ok().build();
    }

    @POST
    @Path("/manglendeFødselsdato")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(description = "Legg til fødsels/dødsdato på åpen/siste behandling", tags = "FORVALTNING-søknad")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response manglendeFødselsdato(@BeanParam @Valid SaksnummerFødselsdatoDto dto) {
        var fagsakId = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(dto.getSaksnummer())).map(Fagsak::getId).orElseThrow();
        var behandling = behandlingRepository.hentÅpneYtelseBehandlingerForFagsakIdForUpdate(fagsakId).stream()
            .filter(SpesialBehandling::erIkkeSpesialBehandling)
            .findFirst().orElseGet(() -> behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(fagsakId).orElseThrow());
        var behandlingId = behandling.getId();
        var gjeldende = familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId);
        if (gjeldende.isEmpty() || !gjeldende.map(FamilieHendelseGrunnlagEntitet::getGjeldendeBarna).orElse(List.of()).isEmpty()
           // || gjeldende.map(FamilieHendelseGrunnlagEntitet::getGjeldendeAntallBarn).filter(ab -> ab > 0).isPresent()
        ) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        var hendelsebuilder = familieHendelseRepository.opprettBuilderForOverstyring(behandlingId)
            .medAntallBarn(1)
            .erFødsel() // Settes til fødsel for å sikre at typen blir fødsel selv om det ikke er født barn.
            .medErMorForSykVedFødsel(null)
            .leggTilBarn(dto.getFødselsdato(), dto.getDødsdato());
        familieHendelseTjeneste.lagreOverstyrtHendelse(behandlingId, hendelsebuilder);

        return Response.ok().build();
    }

    @POST
    @Path("/settNorskIdentAnnenpart")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(description = "Oppdater annen part men kun hvis oppgitt = bruker", tags = "FORVALTNING-søknad")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response settNorskIdentAnnenpart(@BeanParam @Valid SaksnummerAnnenpartIdentDto dto) {
        var fagsakId = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(dto.getSaksnummer())).map(Fagsak::getId).orElseThrow();
        var behandling = behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsakId).stream()
            .filter(SpesialBehandling::erIkkeSpesialBehandling)
                .findFirst().orElseGet(() -> behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId).orElseThrow());
        var oap = personopplysningRepository.hentPersonopplysninger(behandling.getId()).getOppgittAnnenPart().orElseThrow();

        if (dto.getBegrunnelse() == null)
            throw new ForvaltningException("Mangler begrunnelse");
        var nyAktørId = personinfoAdapter.hentAktørForFnr(new PersonIdent(dto.getIdentAnnenPart())).orElseThrow();
        var antall = entityManager.createNativeQuery(
                "UPDATE SO_ANNEN_PART SET AKTOER_ID = :anpa, utl_person_ident = :ident, endret_av = :begr WHERE id = :apid")
                .setParameter("anpa", nyAktørId.getId())
                .setParameter("ident", dto.getIdentAnnenPart())
                .setParameter("apid", oap.getId())
                .setParameter("begr", dto.getBegrunnelse())
                .executeUpdate();
        entityManager.flush();

        return Response.ok(antall).build();
    }

    @POST
    @Path("/settUtlandskIdentAnnenpart")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(description = "Oppdater annen part men kun hvis oppgitt = bruker", tags = "FORVALTNING-søknad")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response settUtlandskIdentAnnenpart(@BeanParam @Valid SaksnummerAnnenpartIdentDto dto) {
        var fagsakId = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(dto.getSaksnummer())).map(Fagsak::getId).orElseThrow();
        var behandling = behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsakId).stream()
            .filter(SpesialBehandling::erIkkeSpesialBehandling)
            .findFirst().orElseGet(() -> behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId).orElseThrow());
        var oap = personopplysningRepository.hentPersonopplysninger(behandling.getId()).getOppgittAnnenPart().orElseThrow();

        var eksisterendeAnnenPart = oap.getAktørId();
        if (oap.getAktørId() != null && !eksisterendeAnnenPart.equals(behandling.getAktørId()))
            throw new ForvaltningException("Støtter bare patching der aktørId er null eller lik bruker i saken");
        var antall = entityManager.createNativeQuery(
                "UPDATE SO_ANNEN_PART SET AKTOER_ID = null, utl_person_ident = :ident, endret_av = :begr WHERE id = :apid")
                .setParameter("ident", dto.getIdentAnnenPart())
                .setParameter("apid", oap.getId())
                .setParameter("begr", dto.getBegrunnelse())
                .executeUpdate();
        entityManager.flush();

        return Response.ok(antall).build();
    }

}
