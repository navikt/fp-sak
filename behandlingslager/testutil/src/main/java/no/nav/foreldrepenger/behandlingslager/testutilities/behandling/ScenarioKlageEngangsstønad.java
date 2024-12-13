package no.nav.foreldrepenger.behandlingslager.testutilities.behandling;

import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType.KLAGE_NFP;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType.KLAGE_VURDER_FORMKRAV_NFP;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.FORESLÅ_VEDTAK;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_NFP;
import static no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering.HJEMSENDE_UTEN_Å_OPPHEVE;
import static no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering.STADFESTE_YTELSESVEDTAK;
import static org.mockito.Mockito.lenient;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageFormkravEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageHjemmel;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageMedholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingBehandlingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingOmgjør;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;

/**
 * Default test scenario builder for Klage Engangssøknad. Kan opprettes for gitt
 * standard Scenario Engangssøknad
 * <p>
 * Oppretter en avsluttet behandling ved hjelp av Scenario Builder.
 * <p>
 * Kan bruke settere (evt. legge til) for å tilpasse utgangspunktet.
 * <p>
 * Mer avansert bruk er ikke gitt at kan bruke denne klassen.
 * <p>
 * Alle scenarioene som har NK resultat, har NFP resultat stadfestet.
 */
public class ScenarioKlageEngangsstønad {

    private final KlageRepository klageRepository = mockKlageRepository();

    public static ScenarioKlageEngangsstønad forFormKrav(AbstractTestScenario<?> abstractTestScenario) {
        return new ScenarioKlageEngangsstønad().setup(abstractTestScenario, KLAGE_VURDER_FORMKRAV_NFP, VURDERING_AV_FORMKRAV_KLAGE_NFP);
    }

    public static ScenarioKlageEngangsstønad forUtenVurderingResultat(AbstractTestScenario<?> abstractTestScenario) {
        return new ScenarioKlageEngangsstønad().setup(abstractTestScenario).medBehandlingStegStart(KLAGE_NFP);
    }

    public static ScenarioKlageEngangsstønad forMedholdNFP(AbstractTestScenario<?> abstractTestScenario) {
        return new ScenarioKlageEngangsstønad().setup(abstractTestScenario, KlageVurdering.MEDHOLD_I_KLAGE);
    }

    public static ScenarioKlageEngangsstønad forAvvistNFP(AbstractTestScenario<?> abstractTestScenario) {
        return new ScenarioKlageEngangsstønad().setup(abstractTestScenario, KlageVurdering.AVVIS_KLAGE);
    }

    public static ScenarioKlageEngangsstønad forMedholdNK(AbstractTestScenario<?> abstractTestScenario) {
        return new ScenarioKlageEngangsstønad().setup(abstractTestScenario, STADFESTE_YTELSESVEDTAK, KlageVurdering.MEDHOLD_I_KLAGE);
    }

    public static ScenarioKlageEngangsstønad forAvvistNK(AbstractTestScenario<?> abstractTestScenario) {
        return new ScenarioKlageEngangsstønad().setup(abstractTestScenario, STADFESTE_YTELSESVEDTAK, KlageVurdering.AVVIS_KLAGE);
    }

    public static ScenarioKlageEngangsstønad forOpphevetNK(AbstractTestScenario<?> abstractTestScenario) {
        return new ScenarioKlageEngangsstønad().setup(abstractTestScenario, STADFESTE_YTELSESVEDTAK, KlageVurdering.OPPHEVE_YTELSESVEDTAK);
    }

    public static ScenarioKlageEngangsstønad forHjemsendtNK(AbstractTestScenario<?> abstractTestScenario) {
        return new ScenarioKlageEngangsstønad().setup(abstractTestScenario, HJEMSENDE_UTEN_Å_OPPHEVE, HJEMSENDE_UTEN_Å_OPPHEVE);
    }

    public static ScenarioKlageEngangsstønad forStadfestetNK(AbstractTestScenario<?> abstractTestScenario) {
        return new ScenarioKlageEngangsstønad().setup(abstractTestScenario, STADFESTE_YTELSESVEDTAK, STADFESTE_YTELSESVEDTAK);
    }

    private final Map<AksjonspunktDefinisjon, BehandlingStegType> opprettedeAksjonspunktDefinisjoner = new EnumMap<>(AksjonspunktDefinisjon.class);
    private final Map<AksjonspunktDefinisjon, BehandlingStegType> utførteAksjonspunktDefinisjoner = new EnumMap<>(AksjonspunktDefinisjon.class);

    private AbstractTestScenario<?> abstractTestScenario;

    private KlageVurdering vurderingNFP;
    private KlageVurdering vurderingNK;
    private String behandlendeEnhet;

    private Behandling klageBehandling;
    private BehandlingStegType startSteg;

    private final KlageVurderingResultat.Builder vurderingResultatNFP = KlageVurderingResultat.builder();
    private final KlageVurderingResultat.Builder vurderingResultatNK = KlageVurderingResultat.builder();

    private KlageResultatEntitet klageResultat;

    private ScenarioKlageEngangsstønad() {
    }

    private ScenarioKlageEngangsstønad setup(AbstractTestScenario<?> abstractTestScenario) {
        return setup(abstractTestScenario, KLAGE_NFP, MANUELL_VURDERING_AV_KLAGE_NFP);
    }

    private ScenarioKlageEngangsstønad setup(AbstractTestScenario<?> abstractTestScenario, BehandlingStegType stegType,
            AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        this.abstractTestScenario = abstractTestScenario;

        // default steg (kan bli overskrevet av andre setup metoder som kaller denne)
        this.startSteg = stegType;

        this.opprettedeAksjonspunktDefinisjoner.put(aksjonspunktDefinisjon, stegType);
        return this;
    }

    private ScenarioKlageEngangsstønad setup(AbstractTestScenario<?> abstractTestScenario, KlageVurdering resultatTypeNFP) {
        setup(abstractTestScenario);
        this.vurderingNFP = resultatTypeNFP;

        this.opprettedeAksjonspunktDefinisjoner.remove(MANUELL_VURDERING_AV_KLAGE_NFP);
        this.utførteAksjonspunktDefinisjoner.put(MANUELL_VURDERING_AV_KLAGE_NFP, KLAGE_NFP);

        // default steg (kan bli overskrevet av andre setup metoder som kaller denne)
        this.startSteg = BehandlingStegType.FORESLÅ_VEDTAK;
        this.opprettedeAksjonspunktDefinisjoner.put(FORESLÅ_VEDTAK, BehandlingStegType.FORESLÅ_VEDTAK);

        // setter default resultat NFP trenger kanskje en utledning fra resultattype
        this.vurderingResultatNFP.medBegrunnelse("DEFAULT")
                .medKlageVurdering(KlageVurdering.AVVIS_KLAGE);
        return this;
    }

    private ScenarioKlageEngangsstønad setup(AbstractTestScenario<?> abstractTestScenario, KlageVurdering resultatTypeNFP,
            KlageVurdering resultatTypeNK) {
        setup(abstractTestScenario, resultatTypeNFP);
        this.vurderingNK = resultatTypeNK;

        // default steg (de fleste scenarioene starter her. De resterende overstyrer i
        // static metoden)
        this.startSteg = BehandlingStegType.FORESLÅ_VEDTAK;
        this.opprettedeAksjonspunktDefinisjoner.put(FORESLÅ_VEDTAK, BehandlingStegType.FORESLÅ_VEDTAK);

        // setter default resultat NFP trenger kanskje en utledning fra resultattype
        this.vurderingResultatNK.medBegrunnelse("DEFAULT")
                .medKlageVurdering(KlageVurdering.AVVIS_KLAGE);
        return this;
    }

    public Behandling lagre(BehandlingRepositoryProvider repositoryProvider, KlageRepository klageRepository) {
        if (klageBehandling != null) {
            throw new IllegalStateException("build allerede kalt.  Hent Behandling via getBehandling eller opprett nytt scenario.");
        }
        abstractTestScenario.buildAvsluttet(repositoryProvider);
        return buildKlage(klageRepository, repositoryProvider);
    }

    private Behandling buildKlage(KlageRepository klageRepository, BehandlingRepositoryProvider repositoryProvider) {
        var fagsak = abstractTestScenario.getFagsak();

        // oppprett og lagre behandling
        var builder = Behandling.forKlage(fagsak);

        if (behandlendeEnhet != null) {
            builder.medBehandlendeEnhet(new OrganisasjonsEnhet(behandlendeEnhet, null));
        }
        klageBehandling = builder.build();
        var behandlingRepository = repositoryProvider.getBehandlingRepository();
        var lås = behandlingRepository.taSkriveLås(klageBehandling);
        behandlingRepository.lagre(klageBehandling, lås);
        klageResultat = klageRepository.hentEvtOpprettKlageResultat(klageBehandling.getId());
        klageRepository.lagreFormkrav(klageBehandling, opprettFormkrav(KlageVurdertAv.NFP));
        if (vurderingNFP != null) {
            klageRepository.lagreVurderingsResultat(klageBehandling,
                    vurderingResultatNFP.medKlageVurdertAv(KlageVurdertAv.NFP).medKlageVurdering(vurderingNFP)
                            .medKlageResultat(klageResultat));
        }
        if (vurderingNK != null) {
            klageRepository.lagreVurderingsResultat(klageBehandling,
                    vurderingResultatNK.medKlageVurdertAv(KlageVurdertAv.NK).medKlageVurdering(vurderingNK)
                            .medKlageResultat(klageResultat));
        }
        if (vurderingNFP != null) {
            Behandlingsresultat.builder().medBehandlingResultatType(
                    KlageVurderingBehandlingResultat.tolkBehandlingResultatType(vurderingNK != null ? vurderingNK : vurderingNFP, KlageVurderingOmgjør.GUNST_MEDHOLD_I_KLAGE, false))
                    .buildFor(klageBehandling);
        } else {
            Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.IKKE_FASTSATT)
                    .buildFor(klageBehandling);
        }

        utførteAksjonspunktDefinisjoner.forEach((apDef, stegType) -> AksjonspunktTestSupport.leggTilAksjonspunkt(klageBehandling, apDef, stegType));

        klageBehandling.getAksjonspunkter().forEach(punkt -> AksjonspunktTestSupport.setTilUtført(punkt, "Test"));

        opprettedeAksjonspunktDefinisjoner
                .forEach((apDef, stegType) -> AksjonspunktTestSupport.leggTilAksjonspunkt(klageBehandling, apDef, stegType));

        if (startSteg != null) {
            InternalManipulerBehandling.forceOppdaterBehandlingSteg(klageBehandling, startSteg);
        }

        return klageBehandling;
    }

    public ScenarioKlageEngangsstønad medKlageMedholdÅrsak(KlageMedholdÅrsak klageMedholdÅrsak) {
        vurderingResultatNFP.medKlageMedholdÅrsak(klageMedholdÅrsak);
        vurderingResultatNK.medKlageMedholdÅrsak(klageMedholdÅrsak);
        return this;
    }

    public ScenarioKlageEngangsstønad medKlageVurderingOmgjør(KlageVurderingOmgjør klageVurderingOmgjør) {
        vurderingResultatNFP.medKlageVurderingOmgjør(klageVurderingOmgjør);
        vurderingResultatNK.medKlageVurderingOmgjør(klageVurderingOmgjør);
        return this;
    }

    public ScenarioKlageEngangsstønad medKlageHjemmel(KlageHjemmel hjemmel) {
        vurderingResultatNFP.medKlageHjemmel(hjemmel);
        vurderingResultatNK.medKlageHjemmel(hjemmel);
        return this;
    }

    public ScenarioKlageEngangsstønad medBegrunnelse(String begrunnelse) {
        vurderingResultatNFP.medBegrunnelse(begrunnelse);
        vurderingResultatNK.medBegrunnelse(begrunnelse);
        return this;
    }

    public ScenarioKlageEngangsstønad medBehandlendeEnhet(String behandlendeEnhet) {
        this.behandlendeEnhet = behandlendeEnhet;
        return this;
    }

    public BehandlingRepository mockBehandlingRepository() {
        var behandlingRepository = abstractTestScenario.mockBehandlingRepository();
        lenient().when(behandlingRepository.hentBehandling(klageBehandling.getId())).thenReturn(klageBehandling);
        return behandlingRepository;
    }

    public BehandlingRepositoryProvider mockBehandlingRepositoryProvider() {
        mockBehandlingRepository();
        return abstractTestScenario.mockBehandlingRepositoryProvider();
    }

    public Behandling lagMocked() {
        // pga det ikke går ann å flytte steg hvis mocket så settes startsteg til null
        startSteg = null;
        var repositoryProvider = abstractTestScenario.mockBehandlingRepositoryProvider();
        lagre(repositoryProvider, this.klageRepository);
        klageBehandling.setId(AbstractTestScenario.nyId());
        return klageBehandling;
    }

    public Fagsak getFagsak() {
        return abstractTestScenario.getFagsak();
    }

    public ScenarioKlageEngangsstønad medAksjonspunkt(AksjonspunktDefinisjon apDef, BehandlingStegType stegType) {
        opprettedeAksjonspunktDefinisjoner.put(apDef, stegType);
        return this;
    }

    public ScenarioKlageEngangsstønad medUtførtAksjonspunkt(AksjonspunktDefinisjon apDef, BehandlingStegType stegType) {
        utførteAksjonspunktDefinisjoner.put(apDef, stegType);
        return this;
    }

    public ScenarioKlageEngangsstønad medBehandlingStegStart(BehandlingStegType startSteg) {
        this.startSteg = startSteg;
        return this;
    }

    private KlageFormkravEntitet.Builder opprettFormkrav(KlageVurdertAv klageVurdertAv) {
        return KlageFormkravEntitet.builder()
                .medErKlagerPart(true)
                .medErFristOverholdt(true)
                .medErKonkret(true)
                .medErSignert(true)
                .medGjelderVedtak(true)
                .medBegrunnelse("Begrunnelse")
                .medKlageResultat(klageResultat)
                .medKlageVurdertAv(klageVurdertAv);
    }

    public KlageRepository getKlageRepository() {
        return klageRepository;
    }

    private KlageRepository mockKlageRepository() {
        return new KlageRepository() {
            private KlageResultatEntitet klageResultat;
            private KlageVurderingResultat klageVurderingResultatNfp = null;
            private KlageVurderingResultat klageVurderingResultatKa = null;
            private KlageFormkravEntitet klageFormkrav;

            @Override
            public KlageResultatEntitet hentEvtOpprettKlageResultat(Long klageBehandlingId) {
                if (klageResultat == null) {
                    this.klageResultat = KlageResultatEntitet.builder().medKlageBehandlingId(klageBehandlingId).build();
                }
                return klageResultat;
            }

            @Override
            public Optional<KlageResultatEntitet> hentKlageResultatHvisEksisterer(Long klageBehandlingId) {
                if (klageResultat == null) {
                    this.klageResultat = KlageResultatEntitet.builder().medKlageBehandlingId(klageBehandlingId).build();
                }
                return Optional.of(klageResultat);
            }

            @Override
            public void lagreVurderingsResultat(Behandling klageBehandling, KlageVurderingResultat.Builder klageVurderingResultatBuilder) {
                var vurderingResultat = klageVurderingResultatBuilder.medKlageResultat(klageResultat).build();
                if (vurderingResultat.getKlageVurdertAv() == KlageVurdertAv.NFP) {
                    klageVurderingResultatNfp = vurderingResultat;
                } else {
                    klageVurderingResultatKa = vurderingResultat;
                }
            }

            @Override
            public Optional<KlageVurderingResultat> hentGjeldendeKlageVurderingResultat(Behandling klageBehandling) {
                if (klageVurderingResultatNfp != null || klageVurderingResultatKa != null) {
                    return klageVurderingResultatKa != null ? Optional.ofNullable(klageVurderingResultatKa)
                            : Optional.ofNullable(klageVurderingResultatNfp);
                }
                return Optional.empty();
            }

            @Override
            public Optional<KlageVurderingResultat> hentKlageVurderingResultat(Long klageBehandlingId, KlageVurdertAv klageVurdertAv) {
                return klageVurdertAv == KlageVurdertAv.NFP ? Optional.ofNullable(klageVurderingResultatNfp)
                        : Optional.ofNullable(klageVurderingResultatKa);
            }

            @Override
            public Optional<KlageFormkravEntitet> hentKlageFormkrav(Long klageBehandling, KlageVurdertAv klageVurdertAv) {
                return Optional.ofNullable(this.klageFormkrav);
            }

            @Override
            public Optional<KlageFormkravEntitet> hentGjeldendeKlageFormkrav(Long behandling) {
                return Optional.ofNullable(this.klageFormkrav);
            }

            @Override
            public void lagreFormkrav(Behandling klageBehandling, KlageFormkravEntitet.Builder klageFormkravBuilder) {
                this.klageFormkrav = klageFormkravBuilder.build();
            }

            @Override
            public void settPåklagdBehandlingId(Long klageBehandlingId, Long påKlagdBehandlingId) {
                var klageResultat = hentEvtOpprettKlageResultat(klageBehandlingId);
                klageResultat.settPåKlagdBehandlingId(påKlagdBehandlingId);
            }
        };
    }
}
