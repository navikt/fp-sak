package no.nav.foreldrepenger.domene.person.pdl;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;

import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.pdl.HentIdenterQueryRequest;
import no.nav.pdl.IdentGruppe;
import no.nav.pdl.IdentInformasjon;
import no.nav.pdl.IdentInformasjonResponseProjection;
import no.nav.pdl.IdentlisteResponseProjection;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.exception.VLException;
import no.nav.vedtak.felles.integrasjon.person.Persondata;
import no.nav.vedtak.util.LRUCache;

@ApplicationScoped
public class AktørTjeneste {

    private static final int DEFAULT_CACHE_SIZE = 3000;
    private static final long DEFAULT_CACHE_TIMEOUT = TimeUnit.MILLISECONDS.convert(8, TimeUnit.HOURS);

    private static final LRUCache<AktørId, PersonIdent> CACHE_AKTØR_ID_TIL_IDENT = new LRUCache<>(DEFAULT_CACHE_SIZE, DEFAULT_CACHE_TIMEOUT);
    private static final LRUCache<PersonIdent, AktørId> CACHE_IDENT_TIL_AKTØR_ID = new LRUCache<>(DEFAULT_CACHE_SIZE, DEFAULT_CACHE_TIMEOUT);

    private Persondata pdlKlient;

    AktørTjeneste() {
        // CDI
    }

    @Inject
    public AktørTjeneste(Persondata pdlKlient) {
        this.pdlKlient = pdlKlient;
    }

    public Optional<AktørId> hentAktørIdForPersonIdent(PersonIdent personIdent) {
        var fraCache = CACHE_IDENT_TIL_AKTØR_ID.get(personIdent);
        if (fraCache != null) {
            CACHE_IDENT_TIL_AKTØR_ID.put(personIdent, fraCache);
            return Optional.of(fraCache);
        }
        var aktørId = hentAktørIdForPersonIdentNonCached(personIdent);
        aktørId.ifPresent(a -> CACHE_IDENT_TIL_AKTØR_ID.put(personIdent, a)); // Kan ikke legge til i cache aktørId -> ident ettersom ident kan være ikke-current
        return aktørId;
    }

    public Optional<AktørId> hentAktørIdForPersonIdentNonCached(PersonIdent personIdent) {
        var request = new HentIdenterQueryRequest();
        request.setIdent(personIdent.getIdent());
        request.setGrupper(List.of(IdentGruppe.AKTORID));
        request.setHistorikk(Boolean.FALSE);
        var projection = new IdentlisteResponseProjection()
                .identer(new IdentInformasjonResponseProjection().ident());

        try {
            var identliste = pdlKlient.hentIdenter(request, projection);
            return identliste.getIdenter().stream().findFirst().map(IdentInformasjon::getIdent).map(AktørId::new);
        } catch (VLException v) {
            if (Persondata.PDL_KLIENT_NOT_FOUND_KODE.equals(v.getKode())) {
                return Optional.empty();
            }
            throw v;
        } catch (ProcessingException e) {
            throw e.getCause() instanceof SocketTimeoutException ? new IntegrasjonException("FP-723618", "PDL timeout") : e;
        }
    }

    public Optional<PersonIdent> hentPersonIdentForAktørId(AktørId aktørId) {
        var fraCache = CACHE_AKTØR_ID_TIL_IDENT.get(aktørId);
        if (fraCache != null) {
            CACHE_AKTØR_ID_TIL_IDENT.put(aktørId, fraCache);
            CACHE_IDENT_TIL_AKTØR_ID.put(fraCache, aktørId); // OK her, men ikke over ettersom dette er gjeldende mapping
            return Optional.of(fraCache);
        }
        var ident = hentPersonIdentForAktørIdNonCached(aktørId);
        ident.ifPresent(i -> {
            CACHE_AKTØR_ID_TIL_IDENT.put(aktørId, i);
            CACHE_IDENT_TIL_AKTØR_ID.put(i, aktørId); // OK her, men ikke over ettersom dette er gjeldende mapping
        });
        return ident;
    }

    public Optional<PersonIdent> hentPersonIdentForAktørIdNonCached(AktørId aktørId) {
        var request = new HentIdenterQueryRequest();
        request.setIdent(aktørId.getId());
        request.setGrupper(List.of(IdentGruppe.FOLKEREGISTERIDENT, IdentGruppe.NPID));
        request.setHistorikk(Boolean.FALSE);
        var projection = new IdentlisteResponseProjection()
                .identer(new IdentInformasjonResponseProjection().ident());


        try {
            var identliste = pdlKlient.hentIdenter(request, projection);
            return identliste.getIdenter().stream().findFirst().map(IdentInformasjon::getIdent).map(PersonIdent::new);
        } catch (VLException v) {
            if (Persondata.PDL_KLIENT_NOT_FOUND_KODE.equals(v.getKode())) {
                return Optional.empty();
            }
            throw v;
        } catch (ProcessingException e) {
            throw e.getCause() instanceof SocketTimeoutException ? new IntegrasjonException("FP-723618", "PDL timeout") : e;
        }

    }
}
