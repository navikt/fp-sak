package no.nav.foreldrepenger.behandlingslager.hendelser;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.jpa.VLPersistenceUnit;

@ApplicationScoped
public class HendelseSorteringRepository {

    private EntityManager entityManager;

    HendelseSorteringRepository() {
        // CDI
    }

    @Inject
    public HendelseSorteringRepository(@VLPersistenceUnit EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public List<AktørId> hentEksisterendeAktørIderMedSak(List<AktørId> aktørIdListe) {
        Set<AktørId> aktørIdSet = aktørIdListe.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if (aktørIdSet.isEmpty()) {
            return Collections.emptyList();
        }

        List<AktørId> resultat1 = getAktørIderMedRelevantSak(aktørIdSet).getResultList();
        List<AktørId> resultat2 = getAktørIderSomErBarnIRelevantForeldrepengesak(aktørIdSet).getResultList();
        Stream<AktørId> resultater = Stream.concat(resultat1.stream(), resultat2.stream());

        return resultater
            .sorted()
            .distinct()
            .collect(Collectors.toList());
    }

    private TypedQuery<AktørId> getAktørIderMedRelevantSak(Set<AktørId> aktørIdListe) {
        TypedQuery<AktørId> query = entityManager.createQuery(
            "select b.aktørId from Bruker b " +
                "inner join Fagsak f on b = f.navBruker " +
                "where b.aktørId in (:aktørIder) " +
                "and ((f.fagsakStatus != :fagsakStatus " +
                "and f.ytelseType != :ytelseType) " +
                "or f.ytelseType = :ytelseType) ", //$NON-NLS-1$
            AktørId.class);
        query.setParameter("aktørIder", aktørIdListe);
        query.setParameter("fagsakStatus", FagsakStatus.AVSLUTTET);
        query.setParameter("ytelseType", FagsakYtelseType.ENGANGSTØNAD);
        return query;
    }

    private TypedQuery<AktørId> getAktørIderSomErBarnIRelevantForeldrepengesak(Set<AktørId> aktørIdListe) {
        TypedQuery<AktørId> query = entityManager.createQuery(
            "select distinct por.tilAktørId from PersonopplysningGrunnlagEntitet gr " +
                "inner join PersonInformasjon poi on gr.registrertePersonopplysninger = poi " +
                "inner join PersonopplysningRelasjon por on por.personopplysningInformasjon = poi " +
                "inner join Behandling b on gr.behandlingId = b.id " +
                "inner join Fagsak f on b.fagsak = f " +
                "where por.relasjonsrolle = :relasjonsRolle " +
                "and por.tilAktørId in (:aktørIder) " +
                "and gr.aktiv = :aktiv " +
                "and f.fagsakStatus != :fagsakStatus " +
                "and f.ytelseType = :ytelseType ", //$NON-NLS-1$
            AktørId.class);
        query.setParameter("relasjonsRolle", RelasjonsRolleType.BARN);
        query.setParameter("aktørIder", aktørIdListe);
        query.setParameter("aktiv", true);
        query.setParameter("fagsakStatus", FagsakStatus.AVSLUTTET);
        query.setParameter("ytelseType", FagsakYtelseType.FORELDREPENGER);
        return query;
    }
}
