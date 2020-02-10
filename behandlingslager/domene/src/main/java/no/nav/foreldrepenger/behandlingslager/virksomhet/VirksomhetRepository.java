package no.nav.foreldrepenger.behandlingslager.virksomhet;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import no.nav.vedtak.util.LRUCache;

/** Cacher oppslag på virksomhet i minne nå (istdf. å lagre i database.) */
@ApplicationScoped
public class VirksomhetRepository {

    private final Virksomhet KUNSTIG_VIRKSOMHET = new VirksomhetEntitet.Builder()
        .medNavn("Kunstig virksomhet")
        .medOrganisasjonstype(Organisasjonstype.KUNSTIG)
        .medOrgnr(OrgNummer.KUNSTIG_ORG)
        .medRegistrert(LocalDate.of(1978, 01, 01))
        .medOppstart(LocalDate.of(1978, 01, 01))
        .build();
    
    private LRUCache<String, Virksomhet> cache = new LRUCache<>(2000, 24 * 60 * 60 * 1000);

    public VirksomhetRepository() {
    }

    public Optional<Virksomhet> hent(String orgnr) {
        if (Objects.equals(KUNSTIG_VIRKSOMHET.getOrgnr(), orgnr)) {
            return Optional.of(KUNSTIG_VIRKSOMHET);
        }
        return Optional.ofNullable(cache.get(orgnr));
    }

    public void lagre(Virksomhet virksomhet) {
        cache.put(virksomhet.getOrgnr(), virksomhet);
    }


}