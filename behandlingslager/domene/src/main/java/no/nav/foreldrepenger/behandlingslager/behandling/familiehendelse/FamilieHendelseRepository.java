package no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.TypedQuery;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.RegisterdataDiffsjekker;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.diff.DiffResult;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;
import no.nav.vedtak.felles.jpa.VLPersistenceUnit;

@ApplicationScoped
public class FamilieHendelseRepository {

    private EntityManager entityManager;
    private BehandlingLåsRepository behandlingLåsRepository;

    protected FamilieHendelseRepository() {
        // CDI proxy
    }

    @Inject
    public FamilieHendelseRepository(@VLPersistenceUnit EntityManager entityManager, BehandlingLåsRepository behandlingLåsRepository) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$ // NOSONAR
        this.entityManager = entityManager;
        this.behandlingLåsRepository = behandlingLåsRepository;
    }

    public FamilieHendelseRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
        if (entityManager != null) {
            this.behandlingLåsRepository = new BehandlingLåsRepository(entityManager);
        }
    }

    public FamilieHendelseGrunnlagEntitet hentAggregat(Long behandlingId) {
        final Optional<FamilieHendelseGrunnlagEntitet> aktivtFamilieHendelseGrunnlag = getAktivtFamilieHendelseGrunnlag(behandlingId);
        if (aktivtFamilieHendelseGrunnlag.isPresent()) {
            return aktivtFamilieHendelseGrunnlag.get();
        }
        throw FamilieHendelseFeil.FACTORY.fantIkkeForventetGrunnlagPåBehandling(behandlingId).toException();
    }

    public Optional<FamilieHendelseGrunnlagEntitet> hentAggregatHvisEksisterer(Long behandlingId) {
        final Optional<FamilieHendelseGrunnlagEntitet> aktivtFamilieHendelseGrunnlag = getAktivtFamilieHendelseGrunnlag(behandlingId);
        return aktivtFamilieHendelseGrunnlag.isPresent() ? Optional.of(aktivtFamilieHendelseGrunnlag.get()) : Optional.empty();
    }

    public DiffResult diffResultat(FamilieHendelseGrunnlagEntitet grunnlag1, FamilieHendelseGrunnlagEntitet grunnlag2, boolean onlyCheckTrackedFields) {
        return new RegisterdataDiffsjekker(onlyCheckTrackedFields).getDiffEntity().diff(grunnlag1, grunnlag2);
    }

    private Optional<FamilieHendelseGrunnlagEntitet> getAktivtFamilieHendelseGrunnlag(Long behandlingId) {
        final TypedQuery<FamilieHendelseGrunnlagEntitet> query = entityManager.createQuery("FROM FamilieHendelseGrunnlag gr " + // NOSONAR //$NON-NLS-1$
            "WHERE gr.behandlingId = :behandlingId " + //$NON-NLS-1$
            "AND gr.aktiv = :aktivt", FamilieHendelseGrunnlagEntitet.class)
            .setFlushMode(FlushModeType.COMMIT); //$NON-NLS-1$
        query.setParameter("behandlingId", behandlingId); // NOSONAR //$NON-NLS-1$
        query.setParameter("aktivt", true); // NOSONAR //$NON-NLS-1$
        return HibernateVerktøy.hentUniktResultat(query);
    }

    private void lagreOgFlush(Long behandlingId, FamilieHendelseGrunnlagEntitet nyttGrunnlag) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR $NON-NLS-1$ //$NON-NLS-1$
        if (nyttGrunnlag == null) {
            return;
        }
        final BehandlingLås lås = behandlingLåsRepository.taLås(behandlingId);
        final Optional<FamilieHendelseGrunnlagEntitet> tidligereAggregat = getAktivtFamilieHendelseGrunnlag(behandlingId);

        if (tidligereAggregat.isPresent()) {
            final FamilieHendelseGrunnlagEntitet aggregat = tidligereAggregat.get();
            if (!diffResultat(aggregat, nyttGrunnlag, true).isEmpty()) {
                aggregat.setAktiv(false);
                entityManager.persist(aggregat);
                entityManager.flush();
                lagreGrunnlag(nyttGrunnlag, behandlingId);
            }
        } else {
            lagreGrunnlag(nyttGrunnlag, behandlingId);
        }
        verifiserBehandlingLås(lås);
        entityManager.flush();
    }

    private void lagreGrunnlag(FamilieHendelseGrunnlagEntitet nyttGrunnlag, Long behandlingId) {
        nyttGrunnlag.setBehandling(behandlingId);
        lagreHendelse(nyttGrunnlag.getSøknadVersjon());

        if (nyttGrunnlag.getBekreftetVersjon().isPresent()) {
            lagreHendelse(nyttGrunnlag.getBekreftetVersjon().get());
        }

        if (nyttGrunnlag.getOverstyrtVersjon().isPresent()) {
            final FamilieHendelseEntitet entity = nyttGrunnlag.getOverstyrtVersjon().get();
            lagreHendelse(entity);
        }

        entityManager.persist(nyttGrunnlag);
    }

    private void lagreHendelse(FamilieHendelseEntitet entity) {
        entityManager.persist(entity);
        if (entity.getTerminbekreftelse().isPresent()) {
            entityManager.persist(entity.getTerminbekreftelse().get());
        }
        if (entity.getAdopsjon().isPresent()) {
            entityManager.persist(entity.getAdopsjon().get());
        }
        for (UidentifisertBarn uidentifisertBarn : entity.getBarna()) {
            entityManager.persist(uidentifisertBarn);
        }
    }

    public void lagre(Behandling behandling, FamilieHendelseBuilder hendelseBuilder) {
        Objects.requireNonNull(behandling, "behandling"); // NOSONAR //$NON-NLS-1$
        lagre(behandling.getId(), hendelseBuilder);
    }

    public void lagre(Long behandlingId, FamilieHendelseBuilder hendelseBuilder) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR //$NON-NLS-1$
        Objects.requireNonNull(hendelseBuilder, "hendelseBuilder"); // NOSONAR $NON-NLS-1$ //$NON-NLS-1$

        var aggregatBuilder = opprettAggregatBuilderFor(behandlingId);
        var type = hendelseBuilder.getType();
        switch (type) {
            case SØKNAD:
                aggregatBuilder.medSøknadVersjon(hendelseBuilder);
                break;
            case BEKREFTET:
                aggregatBuilder.medBekreftetVersjon(hendelseBuilder);
                break;
            case OVERSTYRT:
                aggregatBuilder.medOverstyrtVersjon(hendelseBuilder);
                break;
            default:
                throw new IllegalArgumentException("Støtter ikke HendelseVersjonType: " + type);
        }
        lagreOgFlush(behandlingId, aggregatBuilder.build());
    }

    public void lagreRegisterHendelse(Behandling behandling, FamilieHendelseBuilder hendelse) {
        Objects.requireNonNull(behandling, "behandling"); // NOSONAR $NON-NLS-1$ //$NON-NLS-1$
        Objects.requireNonNull(hendelse, "hendelse"); // NOSONAR $NON-NLS-1$ //$NON-NLS-1$

        final FamilieHendelseGrunnlagBuilder aggregatBuilder = opprettAggregatBuilderFor(behandling.getId());
        // Fjern overstyr manglende fødsel i tilfelle første innhenting. Bevarer senere justering av dato
        if (erFørsteFødselRegistreringHarTidligereOverstyrtFødsel(aggregatBuilder.getKladd())) {
            aggregatBuilder.medOverstyrtVersjon(null);
        }
        aggregatBuilder.medBekreftetVersjon(hendelse);
        // nullstill overstyring ved overgang fra termin til fødsel
        if (harOverstyrtTerminOgOvergangTilFødsel(aggregatBuilder.getKladd())) {
            aggregatBuilder.medOverstyrtVersjon(null);
        }
        lagreOgFlush(behandling.getId(), aggregatBuilder.build());
    }

    private boolean erFørsteFødselRegistreringHarTidligereOverstyrtFødsel(FamilieHendelseGrunnlagEntitet kladd) {
        final FamilieHendelseType overstyrtHendelseType = kladd.getOverstyrtVersjon()
            .map(FamilieHendelseEntitet::getType).orElse(FamilieHendelseType.UDEFINERT);
        return !kladd.getHarRegisterData() && kladd.getHarOverstyrteData() && FamilieHendelseType.FØDSEL.equals(overstyrtHendelseType);
    }

    private boolean harOverstyrtTerminOgOvergangTilFødsel(FamilieHendelseGrunnlagEntitet kladd) {
        final FamilieHendelseType overstyrtHendelseType = kladd.getOverstyrtVersjon()
            .map(FamilieHendelseEntitet::getType).orElse(FamilieHendelseType.UDEFINERT);
        return kladd.getHarOverstyrteData() && FamilieHendelseType.TERMIN.equals(overstyrtHendelseType)
            && FamilieHendelseType.FØDSEL.equals(kladd.getBekreftetVersjon().map(FamilieHendelseEntitet::getType).orElse(FamilieHendelseType.UDEFINERT));
    }

    public void lagreOverstyrtHendelse(Behandling behandling, FamilieHendelseBuilder hendelse) {
        Objects.requireNonNull(behandling, "behandling"); // NOSONAR //$NON-NLS-1$
        Objects.requireNonNull(hendelse, "hendelse"); // NOSONAR //$NON-NLS-1$

        final FamilieHendelseGrunnlagBuilder aggregatBuilder = opprettAggregatBuilderFor(behandling.getId());
        aggregatBuilder.medOverstyrtVersjon(hendelse);
        lagreOgFlush(behandling.getId(), aggregatBuilder.build());
    }

    private void fjernBekreftetData(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR //$NON-NLS-1$
        final Optional<FamilieHendelseGrunnlagEntitet> grunnlag = hentAggregatHvisEksisterer(behandlingId);
        if (!grunnlag.isPresent()) {
            return;
        }
        final FamilieHendelseGrunnlagBuilder aggregatBuilder = opprettAggregatBuilderFor(behandlingId);
        if (grunnlag.get().getOverstyrtVersjon().isPresent()) {
            aggregatBuilder.medOverstyrtVersjon(null);
        }
        lagreOgFlush(behandlingId, aggregatBuilder.build());
    }

    /**
     * Kopierer grunnlag fra en tidligere behandling. Endrer ikke aggregater, en skaper nye referanser til disse.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        final Optional<FamilieHendelseGrunnlagEntitet> familieHendelseGrunnlag = getAktivtFamilieHendelseGrunnlag(gammelBehandlingId);
        if (familieHendelseGrunnlag.isPresent()) {
            final FamilieHendelseGrunnlagEntitet entitet = new FamilieHendelseGrunnlagEntitet(familieHendelseGrunnlag.get());

            lagreOgFlush(nyBehandlingId, entitet);
        }
    }

    /**
     * Kopierer data fra gammel behandling til ny behandling.
     *
     * Fjerner bekreftede og overstyrte data som var for
     *
     * @param gammelBehandling behandlingen det opprettes revurdering på
     * @param nyBehandling revurderings behandlingen
     */
    public void kopierGrunnlagFraEksisterendeBehandlingForRevurdering(Long gammelBehandlingId, Long nyBehandlingId) {
        final Optional<FamilieHendelseGrunnlagEntitet> familieHendelseGrunnlag = getAktivtFamilieHendelseGrunnlag(gammelBehandlingId);
        if (familieHendelseGrunnlag.isPresent()) {
            final FamilieHendelseGrunnlagEntitet entitet = new FamilieHendelseGrunnlagEntitet(familieHendelseGrunnlag.get());
            entitet.setBekreftetHendelse(null);
            entitet.setOverstyrtHendelse(null);

            lagreOgFlush(nyBehandlingId, entitet);
        }
    }

    /**
     * Slette avklart data på en Behandling. Sørger for at samtidige oppdateringer på samme Behandling,
     * eller andre Behandlinger
     * på samme Fagsak ikke kan gjøres samtidig.
     *
     * @see BehandlingLås
     */
    public void slettAvklarteData(Long behandlingId, BehandlingLås lås) {
        fjernBekreftetData(behandlingId);

        verifiserBehandlingLås(lås);
        getEntityManager().flush();
    }

    private EntityManager getEntityManager() {
        return entityManager;
    }

    // sjekk lås og oppgrader til skriv
    protected void verifiserBehandlingLås(BehandlingLås lås) {
        behandlingLåsRepository.oppdaterLåsVersjon(lås);
    }

    private FamilieHendelseGrunnlagBuilder opprettAggregatBuilderFor(Long behandlingId) {
        Optional<FamilieHendelseGrunnlagEntitet> familieHendelseAggregat = hentAggregatHvisEksisterer(behandlingId);
        return FamilieHendelseGrunnlagBuilder.oppdatere(familieHendelseAggregat);
    }

    public FamilieHendelseBuilder opprettBuilderFor(Behandling behandling) {
        Optional<FamilieHendelseGrunnlagEntitet> familieHendelseAggregat = hentAggregatHvisEksisterer(behandling.getId());
        final FamilieHendelseGrunnlagBuilder oppdatere = FamilieHendelseGrunnlagBuilder.oppdatere(familieHendelseAggregat);
        Objects.requireNonNull(oppdatere, "oppdatere"); //$NON-NLS-1$
        return opprettBuilderFor(Optional.ofNullable(oppdatere.getKladd()), false);
    }

    public FamilieHendelseBuilder opprettBuilderForregister(Behandling behandling) {
        Optional<FamilieHendelseGrunnlagEntitet> familieHendelseAggregat = hentAggregatHvisEksisterer(behandling.getId());
        final FamilieHendelseGrunnlagBuilder oppdatere = FamilieHendelseGrunnlagBuilder.oppdatere(familieHendelseAggregat);
        Objects.requireNonNull(oppdatere, "oppdatere"); //$NON-NLS-1$
        return opprettBuilderFor(Optional.ofNullable(oppdatere.getKladd()), true);
    }

    /**
     * Baserer seg på typen under seg hvis det ikke finnes en tilsvarende.
     * F.eks Ber du om Overstyrt så vil denne basere seg på Hendelse i følgende rekkefølge
     * 1. Overstyrt
     * 2. Bekreftet
     * 3. Søknad
     *
     * @param aggregat nåværende aggregat
     * @return Builder
     */
    private FamilieHendelseBuilder opprettBuilderFor(Optional<FamilieHendelseGrunnlagEntitet> aggregat, boolean register) {
        Objects.requireNonNull(aggregat, "aggregat"); // NOSONAR //$NON-NLS-1$
        if (aggregat.isPresent()) {
            HendelseVersjonType type = register ? HendelseVersjonType.BEKREFTET : utledTypeFor(aggregat);
            final FamilieHendelseGrunnlagEntitet hendelseAggregat = aggregat.get();
            final FamilieHendelseBuilder hendelseAggregat1 = getFamilieHendelseBuilderForType(hendelseAggregat, type);
            if (hendelseAggregat1 != null) {
                hendelseAggregat1.setType(type);
                return hendelseAggregat1;
            }
            throw FamilieHendelseFeil.FACTORY.ukjentVersjonstype().toException();
        }
        throw FamilieHendelseFeil.FACTORY.aggregatKanIkkeVæreNull().toException();
    }

    private FamilieHendelseBuilder getFamilieHendelseBuilderForType(FamilieHendelseGrunnlagEntitet aggregat, HendelseVersjonType type) {
        switch (type) {
            case SØKNAD:
                return FamilieHendelseBuilder.oppdatere(Optional.ofNullable(aggregat.getSøknadVersjon()), type);
            case BEKREFTET:
                if (aggregat.getBekreftetVersjon().isEmpty()) {
                    return getFamilieHendelseBuilderForType(aggregat, HendelseVersjonType.SØKNAD);
                } else {
                    return FamilieHendelseBuilder.oppdatere(aggregat.getBekreftetVersjon(), type);
                }
            case OVERSTYRT:
                if (aggregat.getOverstyrtVersjon().isEmpty()) {
                    return getFamilieHendelseBuilderForType(aggregat, HendelseVersjonType.BEKREFTET);
                } else {
                    return FamilieHendelseBuilder.oppdatere(aggregat.getOverstyrtVersjon(), type);
                }
            default:
                throw new IllegalArgumentException("Støtter ikke HendelseVersjonType: " + type);
        }
    }

    private HendelseVersjonType utledTypeFor(Optional<FamilieHendelseGrunnlagEntitet> aggregat) {
        if (aggregat.isPresent()) {
            if (aggregat.get().getHarOverstyrteData()) {
                return HendelseVersjonType.OVERSTYRT;
            } else if (aggregat.get().getHarBekreftedeData() || aggregat.get().getSøknadVersjon() != null) {
                return HendelseVersjonType.BEKREFTET;
            } else if (aggregat.get().getSøknadVersjon() == null) {
                return HendelseVersjonType.SØKNAD;
            }
            throw new IllegalStateException("Utvikler feil."); //$NON-NLS-1$
        }
        return HendelseVersjonType.SØKNAD;
    }

    public Optional<Long> hentIdPåAktivFamiliehendelse(Long behandlingId) {
        return getAktivtFamilieHendelseGrunnlag(behandlingId)
            .map(FamilieHendelseGrunnlagEntitet::getId);
    }

    public FamilieHendelseGrunnlagEntitet hentFamilieHendelserPåGrunnlagId(Long aggregatId) {
        Optional<FamilieHendelseGrunnlagEntitet> optGrunnlag = getVersjonAvFamiliehendelseGrunnlagPåId(
            aggregatId);
        return optGrunnlag.orElse(null);
    }

    private Optional<FamilieHendelseGrunnlagEntitet> getVersjonAvFamiliehendelseGrunnlagPåId(
                                                                                             Long aggregatId) {
        Objects.requireNonNull(aggregatId, "aggregatId"); // NOSONAR $NON-NLS-1$ //$NON-NLS-1$
        final TypedQuery<FamilieHendelseGrunnlagEntitet> query = entityManager.createQuery("FROM FamilieHendelseGrunnlag gr " + // NOSONAR //$NON-NLS-1$
            "WHERE gr.id = :aggregatId ", FamilieHendelseGrunnlagEntitet.class)
           .setFlushMode(FlushModeType.COMMIT); //$NON-NLS-1$
        query.setParameter("aggregatId", aggregatId); // NOSONAR //$NON-NLS-1$
        return query.getResultStream().findFirst();
    }
}
