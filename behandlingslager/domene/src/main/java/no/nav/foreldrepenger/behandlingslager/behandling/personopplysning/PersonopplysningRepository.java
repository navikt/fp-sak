package no.nav.foreldrepenger.behandlingslager.behandling.personopplysning;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.hibernate.jpa.QueryHints;

import no.nav.foreldrepenger.behandlingslager.TraverseEntityGraphFactory;
import no.nav.foreldrepenger.behandlingslager.behandling.RegisterdataDiffsjekker;
import no.nav.foreldrepenger.behandlingslager.diff.DiffEntity;
import no.nav.foreldrepenger.behandlingslager.diff.DiffResult;
import no.nav.foreldrepenger.behandlingslager.diff.TraverseGraph;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;
import no.nav.vedtak.felles.jpa.VLPersistenceUnit;

/**
 * Dette er et Repository for håndtering av alle persistente endringer i en Personopplysning graf.
 * Personopplysning graf har en rot, representert ved Søkers Personopplysning innslag. Andre innslag kan være Barn eller
 * Partner.
 * <p>
 * Hent opp og lagre innhentende Personopplysning data, fra søknad, register (TPS) eller som avklart av Saksbehandler.
 * Ved hver endring kopieres Personopplysning grafen (inklusiv Familierelasjon) som et felles
 * Aggregat (ref. Domain Driven Design - Aggregat pattern)
 * <p>
 * Søkers Personopplysning representerer rot i grafen. Denne linkes til Behandling gjennom et
 * PersonopplysningGrunnlagEntitet.
 * <p>
 * Merk: standard regler - et Grunnlag eies av en Behandling. Et Aggregat (Søkers Personopplysning graf) har en
 * selvstenig livssyklus og vil kopieres ved hver endring.
 * Ved multiple endringer i et grunnlat for en Behandling vil alltid kun et innslag i grunnlag være aktiv for angitt
 * Behandling.
 */
@ApplicationScoped
public class PersonopplysningRepository {

    private EntityManager entityManager;

    protected PersonopplysningRepository() {
        // CDI
    }

    @Inject
    public PersonopplysningRepository(@VLPersistenceUnit EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Kopierer grunnlag fra en tidligere behandling.  Endrer ikke aggregater, en skaper nye referanser til disse.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Long eksisterendeBehandlingId, Long nyBehandlingId) {
        Optional<PersonopplysningGrunnlagEntitet> eksisterendeGrunnlag = getAktivtGrunnlag(eksisterendeBehandlingId);

        final PersonopplysningGrunnlagBuilder builder = PersonopplysningGrunnlagBuilder.oppdatere(eksisterendeGrunnlag);

        lagreOgFlush(nyBehandlingId, builder);
    }


    public void kopierGrunnlagFraEksisterendeBehandlingForRevurdering(Long eksisterendeBehandlingId, Long nyBehandlingId) {
        Optional<PersonopplysningGrunnlagEntitet> eksisterendeGrunnlag = getAktivtGrunnlag(eksisterendeBehandlingId);

        final PersonopplysningGrunnlagBuilder builder = PersonopplysningGrunnlagBuilder.oppdatere(eksisterendeGrunnlag);
        builder.medOverstyrtVersjon(null);

        lagreOgFlush(nyBehandlingId, builder);
    }

    private DiffEntity personopplysningDiffer() {
        TraverseGraph traverser = TraverseEntityGraphFactory.build();
        return new DiffEntity(traverser);
    }


    public PersonopplysningGrunnlagEntitet hentPersonopplysninger(Long behandlingId) {
        return getAktivtGrunnlag(behandlingId).orElse(null);
    }


    public DiffResult diffResultat(PersonopplysningGrunnlagEntitet grunnlag1, PersonopplysningGrunnlagEntitet grunnlag2, boolean onlyCheckTrackedFields) {
        return new RegisterdataDiffsjekker(onlyCheckTrackedFields).getDiffEntity().diff(grunnlag1, grunnlag2);
    }


    public Optional<PersonopplysningGrunnlagEntitet> hentPersonopplysningerHvisEksisterer(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR //$NON-NLS-1$
        Optional<PersonopplysningGrunnlagEntitet> pbg = getAktivtGrunnlag(behandlingId);
        PersonopplysningGrunnlagEntitet entitet = pbg.orElse(null);
        return Optional.ofNullable(entitet);
    }

    private Optional<PersonopplysningGrunnlagEntitet> getAktivtGrunnlag(Long behandlingId) {
        TypedQuery<PersonopplysningGrunnlagEntitet> query = entityManager.createQuery(
            "SELECT pbg FROM PersonopplysningGrunnlagEntitet pbg WHERE pbg.behandlingId = :behandling_id AND pbg.aktiv = true", // NOSONAR //$NON-NLS-1$
            PersonopplysningGrunnlagEntitet.class)
                .setHint(QueryHints.HINT_CACHE_MODE, "IGNORE")
                .setParameter("behandling_id", behandlingId); // NOSONAR //$NON-NLS-1$

        Optional<PersonopplysningGrunnlagEntitet> resultat = HibernateVerktøy.hentUniktResultat(query);

        populerAktørIdFraBehandling(resultat);
        return resultat;
    }

    private void populerAktørIdFraBehandling(Optional<PersonopplysningGrunnlagEntitet> resultat) {
        resultat.ifPresent(r -> {
            Query aktørQuery = entityManager.createNativeQuery("select br.aktoer_id from bruker br"
                    + " inner join fagsak f on f.bruker_id=br.id"
                    + " inner join behandling be on be.fagsak_id = f.id"
                    + " where be.id = :behandling_id")
                    .setParameter("behandling_id", r.getBehandlingId()); // NOSONAR
            r.setAktørId(new AktørId((String) aktørQuery.getSingleResult()));
        });
    }

    private void lagreOgFlush(Long behandlingId, PersonopplysningGrunnlagBuilder grunnlagBuilder) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR //$NON-NLS-1$
        Objects.requireNonNull(grunnlagBuilder, "grunnlagBuilder"); // NOSONAR //$NON-NLS-1$

        final Optional<PersonopplysningGrunnlagEntitet> aktivtGrunnlag = getAktivtGrunnlag(behandlingId);

        final DiffEntity diffEntity = personopplysningDiffer();

        final PersonopplysningGrunnlagEntitet build = grunnlagBuilder.build();
        build.setBehandlingId(behandlingId);

        if (diffEntity.areDifferent(aktivtGrunnlag.orElse(null), build)) {
            aktivtGrunnlag.ifPresent(grunnlag -> {
                // setter gammelt grunnlag inaktiv. Viktig å gjøre før nye endringer siden vi kun
                // tillater ett aktivt grunnlag per behandling
                grunnlag.setAktiv(false);
                entityManager.persist(grunnlag);
                entityManager.flush();
            });

            build.getRegisterVersjon().ifPresent(this::persisterPersonInformasjon);
            build.getOverstyrtVersjon().ifPresent(this::persisterPersonInformasjon);
            build.getOppgittAnnenPart().ifPresent(oppgittAnnenPart -> entityManager.persist(oppgittAnnenPart));

            entityManager.persist(build);
            entityManager.flush();
        }
    }

    private void persisterPersonInformasjon(PersonInformasjonEntitet registerVersjon) {
        entityManager.persist(registerVersjon);
        for (PersonAdresseEntitet entitet : registerVersjon.getAdresser()) {
            entityManager.persist(entitet);
        }
        for (PersonRelasjonEntitet entitet : registerVersjon.getRelasjoner()) {
            entityManager.persist(entitet);
        }
        for (PersonstatusEntitet entitet : registerVersjon.getPersonstatus()) {
            entityManager.persist(entitet);
        }
        for (StatsborgerskapEntitet entitet : registerVersjon.getStatsborgerskap()) {
            entityManager.persist(entitet);
        }
        for (PersonopplysningEntitet entitet : registerVersjon.getPersonopplysninger()) {
            entityManager.persist(entitet);
        }
    }


    public void lagre(Long behandlingId, PersonInformasjonBuilder builder) {
        Objects.requireNonNull(behandlingId, "behandling"); // NOSONAR //$NON-NLS-1$
        Objects.requireNonNull(builder, "søknadAnnenPartBuilder"); // NOSONAR //$NON-NLS-1$

        final PersonopplysningGrunnlagBuilder nyttGrunnlag = getGrunnlagBuilderFor(behandlingId);

        if (builder.getType().equals(PersonopplysningVersjonType.REGISTRERT)) {
            nyttGrunnlag.medRegistrertVersjon(builder);
        }
        if (builder.getType().equals(PersonopplysningVersjonType.OVERSTYRT)) {
            nyttGrunnlag.medOverstyrtVersjon(builder);
        }

        lagreOgFlush(behandlingId, nyttGrunnlag);
    }

    private PersonopplysningGrunnlagBuilder getGrunnlagBuilderFor(Long behandlingId) {
        final Optional<PersonopplysningGrunnlagEntitet> aktivtGrunnlag = getAktivtGrunnlag(behandlingId);
        return PersonopplysningGrunnlagBuilder.oppdatere(aktivtGrunnlag);
    }


    public void lagre(Long behandlingId, OppgittAnnenPartBuilder oppgittAnnenPart) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR //$NON-NLS-1$
        Objects.requireNonNull(oppgittAnnenPart, "oppgittAnnenPart"); // NOSONAR //$NON-NLS-1$

        final PersonopplysningGrunnlagBuilder nyttGrunnlag = getGrunnlagBuilderFor(behandlingId);

        nyttGrunnlag.medOppgittAnnenPart(oppgittAnnenPart.build());

        lagreOgFlush(behandlingId, nyttGrunnlag);
    }


    public PersonInformasjonBuilder opprettBuilderForRegisterdata(Long behandlingId) {
        final Optional<PersonopplysningGrunnlagEntitet> aktivtGrunnlag = getAktivtGrunnlag(behandlingId);
        return PersonInformasjonBuilder.oppdater(aktivtGrunnlag.flatMap(PersonopplysningGrunnlagEntitet::getRegisterVersjon),
            PersonopplysningVersjonType.REGISTRERT);
    }


    public PersonInformasjonBuilder opprettBuilderForOverstyring(Long behandlingId) {
        final PersonopplysningGrunnlagEntitet aktivtGrunnlag = getAktivtGrunnlag(behandlingId).orElseThrow(IllegalStateException::new);
        return PersonInformasjonBuilder.oppdater(aktivtGrunnlag.getOverstyrtVersjon(),
            PersonopplysningVersjonType.OVERSTYRT);
    }

    private Optional<PersonopplysningGrunnlagEntitet> getInitiellVersjonAvPersonopplysningBehandlingsgrunnlag(
                                                                                                              Long behandlingId) {
        // må også sortere på id da opprettetTidspunkt kun er til nærmeste millisekund og ikke satt fra db.
        TypedQuery<PersonopplysningGrunnlagEntitet> query = entityManager.createQuery(
            "SELECT pbg FROM PersonopplysningGrunnlagEntitet pbg WHERE pbg.behandlingId = :behandling_id order by pbg.opprettetTidspunkt, pbg.id", //$NON-NLS-1$
            PersonopplysningGrunnlagEntitet.class)
                .setParameter("behandling_id", behandlingId) // NOSONAR
                .setMaxResults(1); 

        Optional<PersonopplysningGrunnlagEntitet> resultat = query.getResultStream().findFirst();

        populerAktørIdFraBehandling(resultat);
        return resultat;
    }


    public PersonopplysningGrunnlagEntitet hentFørsteVersjonAvPersonopplysninger(Long behandlingId) {
        Optional<PersonopplysningGrunnlagEntitet> optGrunnlag = getInitiellVersjonAvPersonopplysningBehandlingsgrunnlag(behandlingId);
        return optGrunnlag.orElse(null);
    }


    public PersonopplysningGrunnlagEntitet hentPersonopplysningerPåId(Long aggregatId) {
        Optional<PersonopplysningGrunnlagEntitet> optGrunnlag = getVersjonAvPersonopplysningBehandlingsgrunnlagPåId(
            aggregatId);

        return optGrunnlag.orElse(null);
    }

    private Optional<PersonopplysningGrunnlagEntitet> getVersjonAvPersonopplysningBehandlingsgrunnlagPåId(
                                                                                                          Long aggregatId) {
        TypedQuery<PersonopplysningGrunnlagEntitet> query = entityManager.createQuery(
            "SELECT pbg FROM PersonopplysningGrunnlagEntitet pbg WHERE pbg.id = :aggregatId", //$NON-NLS-1$
            PersonopplysningGrunnlagEntitet.class)
                .setParameter("aggregatId", aggregatId); // NOSONAR //$NON-NLS-1$

        Optional<PersonopplysningGrunnlagEntitet> resultat = query.getResultStream().findFirst();

        populerAktørIdFraBehandling(resultat);
        return resultat;
    }
}
