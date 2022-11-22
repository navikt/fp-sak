package no.nav.foreldrepenger.behandlingslager.behandling.personopplysning;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.hibernate.jpa.QueryHints;

import no.nav.foreldrepenger.behandlingslager.TraverseEntityGraphFactory;
import no.nav.foreldrepenger.behandlingslager.diff.DiffEntity;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

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
    public PersonopplysningRepository( EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Kopierer grunnlag fra en tidligere behandling.  Endrer ikke aggregater, en skaper nye referanser til disse.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Long eksisterendeBehandlingId, Long nyBehandlingId) {
        var eksisterendeGrunnlag = getAktivtGrunnlag(eksisterendeBehandlingId);

        final var builder = PersonopplysningGrunnlagBuilder.oppdatere(eksisterendeGrunnlag);

        lagreOgFlush(nyBehandlingId, builder);
    }


    public void kopierGrunnlagFraEksisterendeBehandlingUtenVurderinger(Long eksisterendeBehandlingId, Long nyBehandlingId) {
        var eksisterendeGrunnlag = getAktivtGrunnlag(eksisterendeBehandlingId);

        final var builder = PersonopplysningGrunnlagBuilder.oppdatere(eksisterendeGrunnlag);
        builder.medOverstyrtVersjon(null);

        lagreOgFlush(nyBehandlingId, builder);
    }

    private DiffEntity personopplysningDiffer() {
        var traverser = TraverseEntityGraphFactory.build();
        return new DiffEntity(traverser);
    }

    public PersonopplysningGrunnlagEntitet hentPersonopplysninger(Long behandlingId) {
        return getAktivtGrunnlag(behandlingId).orElse(null);
    }

    public Optional<PersonopplysningGrunnlagEntitet> hentPersonopplysningerHvisEksisterer(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR //$NON-NLS-1$
        return getAktivtGrunnlag(behandlingId);
    }

    public Optional<OppgittAnnenPartEntitet> hentOppgittAnnenPartHvisEksisterer(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR //$NON-NLS-1$
        var pbg = getAktivtGrunnlag(behandlingId);
        return pbg.flatMap(PersonopplysningGrunnlagEntitet::getOppgittAnnenPart);
    }

    private Optional<PersonopplysningGrunnlagEntitet> getAktivtGrunnlag(Long behandlingId) {
        var query = entityManager.createQuery(
            "SELECT pbg FROM PersonopplysningGrunnlagEntitet pbg WHERE pbg.behandlingId = :behandling_id AND pbg.aktiv = true", // NOSONAR //$NON-NLS-1$
            PersonopplysningGrunnlagEntitet.class)
                .setHint(QueryHints.HINT_CACHE_MODE, "IGNORE")
                .setParameter("behandling_id", behandlingId); // NOSONAR //$NON-NLS-1$

        var resultat = HibernateVerktøy.hentUniktResultat(query);

        populerAktørIdFraBehandling(resultat);
        return resultat;
    }

    private void populerAktørIdFraBehandling(Optional<PersonopplysningGrunnlagEntitet> resultat) {
        resultat.ifPresent(r -> {
            var aktørQuery = entityManager.createNativeQuery("select br.aktoer_id from bruker br"
                    + " inner join fagsak f on f.bruker_id=br.id"
                    + " inner join behandling be on be.fagsak_id = f.id"
                    + " where be.id = :behandling_id")
                    .setParameter("behandling_id", r.getBehandlingId()); // NOSONAR
            r.setAktørId(new AktørId((String) aktørQuery.getSingleResult()));
        });
    }

    public void oppdaterAktørIdFor(AktørId gammel, AktørId gjeldende) {
        utførUpdate(entityManager.createNativeQuery("UPDATE PO_ADRESSE SET AKTOER_ID = :gjeldende WHERE AKTOER_ID = :gammel"), gammel, gjeldende);  //$NON-NLS-1$
        utførUpdate(entityManager.createNativeQuery("UPDATE PO_PERSONOPPLYSNING SET AKTOER_ID = :gjeldende WHERE AKTOER_ID = :gammel"), gammel, gjeldende);  //$NON-NLS-1$
        utførUpdate(entityManager.createNativeQuery("UPDATE PO_PERSONSTATUS SET AKTOER_ID = :gjeldende WHERE AKTOER_ID = :gammel"), gammel, gjeldende);  //$NON-NLS-1$
        utførUpdate(entityManager.createNativeQuery("UPDATE PO_OPPHOLD SET AKTOER_ID = :gjeldende WHERE AKTOER_ID = :gammel"), gammel, gjeldende);  //$NON-NLS-1$
        utførUpdate(entityManager.createNativeQuery("UPDATE PO_STATSBORGERSKAP SET AKTOER_ID = :gjeldende WHERE AKTOER_ID = :gammel"), gammel, gjeldende);  //$NON-NLS-1$
        utførUpdate(entityManager.createNativeQuery("UPDATE PO_RELASJON SET FRA_AKTOER_ID = :gjeldende WHERE FRA_AKTOER_ID = :gammel"), gammel, gjeldende);  //$NON-NLS-1$
        utførUpdate(entityManager.createNativeQuery("UPDATE PO_RELASJON SET TIL_AKTOER_ID = :gjeldende WHERE TIL_AKTOER_ID = :gammel"), gammel, gjeldende);  //$NON-NLS-1$
        utførUpdate(entityManager.createNativeQuery("UPDATE SO_ANNEN_PART SET AKTOER_ID = :gjeldende WHERE AKTOER_ID = :gammel"), gammel, gjeldende);  //$NON-NLS-1$
        entityManager.flush();
    }

    private void utførUpdate(Query query, AktørId gammel, AktørId gjeldende) {
        query.setParameter("gjeldende", gjeldende.getId()); //$NON-NLS-1$
        query.setParameter("gammel", gammel.getId()); //$NON-NLS-1$
        query.executeUpdate();
        entityManager.flush();
    }

    private void lagreOgFlush(Long behandlingId, PersonopplysningGrunnlagBuilder grunnlagBuilder) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR //$NON-NLS-1$
        Objects.requireNonNull(grunnlagBuilder, "grunnlagBuilder"); // NOSONAR //$NON-NLS-1$

        final var aktivtGrunnlag = getAktivtGrunnlag(behandlingId);

        final var diffEntity = personopplysningDiffer();

        final var build = grunnlagBuilder.build();
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
        for (var entitet : registerVersjon.getAdresser()) {
            entityManager.persist(entitet);
        }
        for (var entitet : registerVersjon.getRelasjoner()) {
            entityManager.persist(entitet);
        }
        for (var entitet : registerVersjon.getPersonstatus()) {
            entityManager.persist(entitet);
        }
        for (var entitet : registerVersjon.getOppholdstillatelser()) {
            entityManager.persist(entitet);
        }
        for (var entitet : registerVersjon.getStatsborgerskap()) {
            entityManager.persist(entitet);
        }
        for (var entitet : registerVersjon.getPersonopplysninger()) {
            entityManager.persist(entitet);
        }
    }


    public void lagre(Long behandlingId, PersonInformasjonBuilder builder) {
        Objects.requireNonNull(behandlingId, "behandling"); // NOSONAR //$NON-NLS-1$
        Objects.requireNonNull(builder, "søknadAnnenPartBuilder"); // NOSONAR //$NON-NLS-1$

        final var nyttGrunnlag = getGrunnlagBuilderFor(behandlingId);

        if (builder.getType().equals(PersonopplysningVersjonType.REGISTRERT)) {
            nyttGrunnlag.medRegistrertVersjon(builder);
        }
        if (builder.getType().equals(PersonopplysningVersjonType.OVERSTYRT)) {
            nyttGrunnlag.medOverstyrtVersjon(builder);
        }

        lagreOgFlush(behandlingId, nyttGrunnlag);
    }

    private PersonopplysningGrunnlagBuilder getGrunnlagBuilderFor(Long behandlingId) {
        final var aktivtGrunnlag = getAktivtGrunnlag(behandlingId);
        return PersonopplysningGrunnlagBuilder.oppdatere(aktivtGrunnlag);
    }


    public void lagre(Long behandlingId, OppgittAnnenPartEntitet oppgittAnnenPart) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR //$NON-NLS-1$
        Objects.requireNonNull(oppgittAnnenPart, "oppgittAnnenPart"); // NOSONAR //$NON-NLS-1$

        final var nyttGrunnlag = getGrunnlagBuilderFor(behandlingId);

        nyttGrunnlag.medOppgittAnnenPart(oppgittAnnenPart);

        lagreOgFlush(behandlingId, nyttGrunnlag);
    }


    public PersonInformasjonBuilder opprettBuilderForRegisterdata(Long behandlingId) {
        final var aktivtGrunnlag = getAktivtGrunnlag(behandlingId);
        return PersonInformasjonBuilder.oppdater(aktivtGrunnlag.flatMap(PersonopplysningGrunnlagEntitet::getRegisterVersjon),
            PersonopplysningVersjonType.REGISTRERT);
    }


    private Optional<PersonopplysningGrunnlagEntitet> getInitiellVersjonAvPersonopplysningBehandlingsgrunnlag(
                                                                                                              Long behandlingId) {
        // må også sortere på id da opprettetTidspunkt kun er til nærmeste millisekund og ikke satt fra db.
        var query = entityManager.createQuery(
            "SELECT pbg FROM PersonopplysningGrunnlagEntitet pbg WHERE pbg.behandlingId = :behandling_id order by pbg.opprettetTidspunkt, pbg.id", //$NON-NLS-1$
            PersonopplysningGrunnlagEntitet.class)
                .setParameter("behandling_id", behandlingId) // NOSONAR
                .setMaxResults(1);

        var resultat = query.getResultStream().findFirst();

        populerAktørIdFraBehandling(resultat);
        return resultat;
    }

    public List<Fagsak> fagsakerMedOppgittAnnenPart(AktørId annenPart) {
        var aktørQuery = entityManager.createQuery("select distinct f from Fagsak f "
                + " inner join Behandling b on b.fagsak=f "
                + " join PersonopplysningGrunnlagEntitet gr on gr.behandlingId=b.id "
                + " join SøknadAnnenPart ap on gr.søknadAnnenPart=ap "
                + " where ap.aktørId = :aktoerId and gr.aktiv='J'", Fagsak.class)
            .setParameter("aktoerId", annenPart); // NOSONAR
        return aktørQuery.getResultList();
    }

    public PersonopplysningGrunnlagEntitet hentFørsteVersjonAvPersonopplysninger(Long behandlingId) {
        var optGrunnlag = getInitiellVersjonAvPersonopplysningBehandlingsgrunnlag(behandlingId);
        return optGrunnlag.orElse(null);
    }


    public PersonopplysningGrunnlagEntitet hentGrunnlagPåId(Long grunnlagId) {
        var query = entityManager.createQuery(
            "SELECT pbg FROM PersonopplysningGrunnlagEntitet pbg WHERE pbg.id = :grunnlagId", //$NON-NLS-1$
            PersonopplysningGrunnlagEntitet.class)
                .setParameter("grunnlagId", grunnlagId); // NOSONAR //$NON-NLS-1$

        var resultat = query.getResultStream().findFirst();

        populerAktørIdFraBehandling(resultat);
        return resultat.orElse(null);
    }

}
