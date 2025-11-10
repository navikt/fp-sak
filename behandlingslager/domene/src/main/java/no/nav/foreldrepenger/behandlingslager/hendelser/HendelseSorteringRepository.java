package no.nav.foreldrepenger.behandlingslager.hendelser;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
public class HendelseSorteringRepository {

    private EntityManager entityManager;

    HendelseSorteringRepository() {
        // CDI
    }

    @Inject
    public HendelseSorteringRepository( EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    public Collection<AktørId> hentEksisterendeAktørIderMedSak(Set<AktørId> aktørIdSet) {
        if (aktørIdSet.isEmpty()) {
            return Set.of();
        }

        var resultat1 = getAktørIderMedRelevantSak(aktørIdSet).getResultList();
        var resultat2 = getAktørIderSomErBarnIRelevantForeldrepengesak(aktørIdSet).getResultList();
        var resultater = Stream.concat(resultat1.stream(), resultat2.stream());

        return resultater.collect(Collectors.toSet());
    }

    public Collection<AktørId> hentEksisterendeAktørIderMedHistoriskSak(Set<AktørId> aktørIdSet) {
        if (aktørIdSet.isEmpty()) {
            return List.of();
        }

        return entityManager.createQuery("""
                select b.aktørId from Bruker b
                inner join Fagsak f on b = f.navBruker
                where b.aktørId in (:aktørIds)
                """, AktørId.class)
            .setParameter("aktørIds", aktørIdSet)
            .getResultList();
    }

    public Collection<Saksnummer> hentSakerForAktørIder(Set<AktørId> aktørIdSet) {
        if (aktørIdSet.isEmpty()) {
            return List.of();
        }

        return entityManager.createQuery("""
                select f.saksnummer from Bruker b
                inner join Fagsak f on b = f.navBruker
                where b.aktørId in (:aktørIds)
                """, Saksnummer.class)
            .setParameter("aktørIds", aktørIdSet)
            .getResultList();
    }

    private TypedQuery<AktørId> getAktørIderMedRelevantSak(Set<AktørId> aktørIdSet) {
        var query = entityManager.createQuery("""
                select b.aktørId from Bruker b
                inner join Fagsak f on b = f.navBruker
                where b.aktørId in (:aktørIder)
                and ((f.fagsakStatus != :fagsakStatus and f.ytelseType != :ytelseType) or f.ytelseType = :ytelseType)
                """, AktørId.class);
        query.setParameter("aktørIder", aktørIdSet);
        query.setParameter("fagsakStatus", FagsakStatus.AVSLUTTET);
        query.setParameter("ytelseType", FagsakYtelseType.ENGANGSTØNAD);
        return query;
    }

    private TypedQuery<AktørId> getAktørIderSomErBarnIRelevantForeldrepengesak(Set<AktørId> aktørIdSet) {
        var query = entityManager.createQuery("""
                select distinct por.tilAktørId from PersonopplysningGrunnlagEntitet gr
                inner join PersonInformasjon poi on gr.registrertePersonopplysninger = poi
                inner join PersonopplysningRelasjon por on por.personopplysningInformasjon = poi
                inner join Behandling b on gr.behandlingId = b.id
                inner join Fagsak f on b.fagsak = f
                where por.relasjonsrolle = :relasjonsRolle
                and por.tilAktørId in (:aktørIder)
                and gr.aktiv = :aktiv
                and f.fagsakStatus != :fagsakStatus
                and f.ytelseType = :ytelseType
                """,
            AktørId.class);
        query.setParameter("relasjonsRolle", RelasjonsRolleType.BARN);
        query.setParameter("aktørIder", aktørIdSet);
        query.setParameter("aktiv", true);
        query.setParameter("fagsakStatus", FagsakStatus.AVSLUTTET);
        query.setParameter("ytelseType", FagsakYtelseType.FORELDREPENGER);
        return query;
    }
}
