package no.nav.foreldrepenger.domene.person.pdl;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.pdl.Folkeregisterpersonstatus;
import no.nav.pdl.FolkeregisterpersonstatusResponseProjection;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.PersonResponseProjection;

@ApplicationScoped
public class TilknytningTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(TilknytningTjeneste.class);

    private static final Set<String> IKKE_BOSATT = Set.of("ikkeBosatt", "opphoert", "forsvunnet");

    private PdlKlientLogCause pdlKlient;

    TilknytningTjeneste() {
        // CDI
    }

    @Inject
    public TilknytningTjeneste(PdlKlientLogCause pdlKlient) {
        this.pdlKlient = pdlKlient;
    }

    public boolean erIkkeBosattFreg(FagsakYtelseType ytelseType, AktørId aktørId) {
        var query = new HentPersonQueryRequest();
        query.setIdent(aktørId.getId());
        var projection = new PersonResponseProjection()
            .folkeregisterpersonstatus(new FolkeregisterpersonstatusResponseProjection().forenkletStatus());

        var person = pdlKlient.hentPerson(ytelseType, query, projection);

        var statusIkkeBosatt = person.getFolkeregisterpersonstatus().stream()
            .map(Folkeregisterpersonstatus::getForenkletStatus)
            .anyMatch(IKKE_BOSATT::contains);
        return statusIkkeBosatt;
    }

}
