package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.SaksnummerAnnenpartIdentDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.SaksnummerTermindatoDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

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

    @Inject
    public ForvaltningSøknadRestTjeneste(BehandlingRepositoryProvider repositoryProvider, PersoninfoAdapter personinfoAdapter) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        this.entityManager = repositoryProvider.getEntityManager();
        this.personinfoAdapter = personinfoAdapter;
    }

    public ForvaltningSøknadRestTjeneste() {
        // CDI
    }

    @POST
    @Path("/endreTermindato")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppdater termindato åpen/siste behandling ifm prematur situasjons", tags = "FORVALTNING-søknad")
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK, sporingslogg = false)
    public Response endreTermindato(@BeanParam @Valid SaksnummerTermindatoDto dto) {
        var fagsakId = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(dto.getSaksnummer())).map(Fagsak::getId).orElseThrow();
        var behandling = behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsakId).stream()
                .filter(b -> !b.harBehandlingÅrsak(BehandlingÅrsakType.BERØRT_BEHANDLING))
                .findFirst().orElseGet(() -> behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId).orElseThrow());
        int antall = familieHendelseRepository.oppdaterGjeldendeTermindatoForBehandling(behandling.getId(), dto.getTermindato(),
                dto.getBegrunnelse());

        return Response.ok(antall).build();
    }

    @POST
    @Path("/settNorskIdentAnnenpart")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppdater annen part men kun hvis oppgitt = bruker", tags = "FORVALTNING-søknad")
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK, sporingslogg = false)
    public Response settNorskIdentAnnenpart(@BeanParam @Valid SaksnummerAnnenpartIdentDto dto) {
        var fagsakId = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(dto.getSaksnummer())).map(Fagsak::getId).orElseThrow();
        var behandling = behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsakId).stream()
                .filter(b -> !b.harBehandlingÅrsak(BehandlingÅrsakType.BERØRT_BEHANDLING))
                .findFirst().orElseGet(() -> behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId).orElseThrow());
        var oap = personopplysningRepository.hentPersonopplysninger(behandling.getId()).getOppgittAnnenPart().orElseThrow();

        AktørId eksisterendeAnnenPart = oap.getAktørId();
        if (oap.getAktørId() != null && !eksisterendeAnnenPart.equals(behandling.getAktørId()))
            throw new IllegalArgumentException("Støtter bare patching der aktørId er null eller lik bruker i saken");
        var nyAktørId = personinfoAdapter.hentAktørForFnr(new PersonIdent(dto.getIdentAnnenPart())).orElseThrow();
        int antall = entityManager.createNativeQuery(
                "UPDATE SO_ANNEN_PART SET AKTOER_ID = :anpa, utl_person_ident = :ident, endret_av = :begr WHERE id = :apid")
                .setParameter("anpa", nyAktørId.getId())
                .setParameter("ident", dto.getIdentAnnenPart())
                .setParameter("apid", oap.getId())
                .setParameter("begr", dto.getBegrunnelse())
                .executeUpdate(); // $NON-NLS-1$
        entityManager.flush();

        return Response.ok(antall).build();
    }

    @POST
    @Path("/settUtlandskIdentAnnenpart")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppdater annen part men kun hvis oppgitt = bruker", tags = "FORVALTNING-søknad")
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK, sporingslogg = false)
    public Response settUtlandskIdentAnnenpart(@BeanParam @Valid SaksnummerAnnenpartIdentDto dto) {
        var fagsakId = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(dto.getSaksnummer())).map(Fagsak::getId).orElseThrow();
        var behandling = behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsakId).stream()
                .filter(b -> !b.harBehandlingÅrsak(BehandlingÅrsakType.BERØRT_BEHANDLING))
                .findFirst().orElseGet(() -> behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId).orElseThrow());
        var oap = personopplysningRepository.hentPersonopplysninger(behandling.getId()).getOppgittAnnenPart().orElseThrow();

        AktørId eksisterendeAnnenPart = oap.getAktørId();
        if (oap.getAktørId() != null && !eksisterendeAnnenPart.equals(behandling.getAktørId()))
            throw new IllegalArgumentException("Støtter bare patching der aktørId er null eller lik bruker i saken");
        int antall = entityManager.createNativeQuery(
                "UPDATE SO_ANNEN_PART SET AKTOER_ID = null, utl_person_ident = :ident, endret_av = :begr WHERE id = :apid")
                .setParameter("ident", dto.getIdentAnnenPart())
                .setParameter("apid", oap.getId())
                .setParameter("begr", dto.getBegrunnelse())
                .executeUpdate(); // $NON-NLS-1$
        entityManager.flush();

        return Response.ok(antall).build();
    }

}
