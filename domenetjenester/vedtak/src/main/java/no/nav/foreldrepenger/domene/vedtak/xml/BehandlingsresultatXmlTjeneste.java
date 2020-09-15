package no.nav.foreldrepenger.domene.vedtak.xml;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeAvvistÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeOmgjørÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingOmgjør;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageAvvistÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageFormkravEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageMedholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingOmgjør;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.vedtak.felles.xml.felles.v2.KodeverksOpplysning;
import no.nav.vedtak.felles.xml.vedtak.v2.AnkeAvvistAarsak;
import no.nav.vedtak.felles.xml.vedtak.v2.AnkeOmgjoerAarsak;
import no.nav.vedtak.felles.xml.vedtak.v2.Ankevurdering;
import no.nav.vedtak.felles.xml.vedtak.v2.Ankevurderingresultat;
import no.nav.vedtak.felles.xml.vedtak.v2.Behandlingsresultat;
import no.nav.vedtak.felles.xml.vedtak.v2.Behandlingstype;
import no.nav.vedtak.felles.xml.vedtak.v2.KlageAvvistAarsak;
import no.nav.vedtak.felles.xml.vedtak.v2.KlageMedholdAarsak;
import no.nav.vedtak.felles.xml.vedtak.v2.KlageVurdertAv;
import no.nav.vedtak.felles.xml.vedtak.v2.Klagevurdering;
import no.nav.vedtak.felles.xml.vedtak.v2.Klagevurderingresultat;
import no.nav.vedtak.felles.xml.vedtak.v2.ManuellVurderingsResultat;
import no.nav.vedtak.felles.xml.vedtak.v2.ObjectFactory;
import no.nav.vedtak.felles.xml.vedtak.v2.Vedtak;
import no.nav.vedtak.felles.xml.vedtak.v2.Vilkaar;
import no.nav.vedtak.felles.xml.vedtak.v2.Vilkaarsutfall;
import no.nav.vedtak.felles.xml.vedtak.v2.Vurderingsvariant;
import no.nav.vedtak.felles.xml.vedtak.v2.VurderteVilkaar;

@ApplicationScoped
public class BehandlingsresultatXmlTjeneste {

    private ObjectFactory v2ObjectFactory = new ObjectFactory();
    private Instance<BeregningsresultatXmlTjeneste> beregningsresultatXmlTjeneste;
    private Instance<VilkårsgrunnlagXmlTjeneste> vilkårsgrunnlagXmlTjeneste;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private KlageRepository klageRepository;
    private AnkeRepository ankeRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    public BehandlingsresultatXmlTjeneste() {
        // For CDI
    }

    @Inject
    public BehandlingsresultatXmlTjeneste(@Any Instance<BeregningsresultatXmlTjeneste> beregningsresultatXmlTjeneste,
                                          @Any Instance<VilkårsgrunnlagXmlTjeneste> vilkårsgrunnlagXmlTjeneste,
                                          BehandlingVedtakRepository behandlingVedtakRepository,
                                          KlageRepository klageRepository,
                                          AnkeRepository ankeRepository,
                                          VilkårResultatRepository vilkårResultatRepository) {
        this.beregningsresultatXmlTjeneste = beregningsresultatXmlTjeneste;
        this.vilkårsgrunnlagXmlTjeneste = vilkårsgrunnlagXmlTjeneste;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.klageRepository = klageRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.ankeRepository = ankeRepository;
    }

    public void setBehandlingresultat(Vedtak vedtak, Behandling behandling) {
        Behandlingsresultat behandlingsresultat;
        if (behandling.getType().equals(BehandlingType.KLAGE)) {
            behandlingsresultat = v2ObjectFactory.createKlagevurderingresultat();
            leggTilKlageVerdier((Klagevurderingresultat) behandlingsresultat, behandling);
        } else if (behandling.getType().equals(BehandlingType.ANKE)) {
            behandlingsresultat = v2ObjectFactory.createAnkevurderingresultat();
            leggTilAnkeVerdier((Ankevurderingresultat) behandlingsresultat, behandling);
        } else {
            behandlingsresultat = v2ObjectFactory.createBehandlingsresultat();
            var xmlTjeneste = FagsakYtelseTypeRef.Lookup.find(beregningsresultatXmlTjeneste, behandling.getFagsakYtelseType()).orElseThrow();
            xmlTjeneste.setBeregningsresultat(behandlingsresultat, behandling);
        }
        setBehandlingsresultatType(behandlingsresultat, behandling);
        setBehandlingstype(behandlingsresultat, behandling);
        behandlingsresultat.setBehandlingsId(behandling.getId().toString());
        setVurderteVilkaar(behandlingsresultat, behandling);
        setManuelleVurderinger(behandlingsresultat, behandling);

        vedtak.setBehandlingsresultat(behandlingsresultat);
    }

    private void leggTilKlageVerdier(Klagevurderingresultat klagevurderingresultat, Behandling behandling) {
        Optional<KlageVurderingResultat> optionalGjeldendeKlagevurderingresultat = klageRepository.hentGjeldendeKlageVurderingResultat(behandling);
        Optional<KlageFormkravEntitet> optionalKlageFormkrav = klageRepository.hentGjeldendeKlageFormkrav(behandling.getId());
        if (optionalGjeldendeKlagevurderingresultat.isPresent() && optionalKlageFormkrav.isPresent()) {
            KlageVurderingResultat gjeldendeKlagevurderingresultat = optionalGjeldendeKlagevurderingresultat.get();
            klagevurderingresultat.setKlageAvvistAarsak(hentKlageAvvistårsak(optionalKlageFormkrav.get()));
            klagevurderingresultat.setKlageMedholdAarsak(hentKlageMedholdårsak(gjeldendeKlagevurderingresultat));
            klagevurderingresultat.setKlageVurdering(hentKlagevurdering(gjeldendeKlagevurderingresultat));
            klagevurderingresultat.setKlageVurdertAv(hentKlageVurdertAv(gjeldendeKlagevurderingresultat));
        }
    }

    private KlageVurdertAv hentKlageVurdertAv(KlageVurderingResultat vurderingsresultat) {
        no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv klageVurdertAv = vurderingsresultat.getKlageVurdertAv();
        if (no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv.NFP.equals(klageVurdertAv)) {
            return KlageVurdertAv.NFP;
        } else if (no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv.NK.equals(klageVurdertAv)) {
            return KlageVurdertAv.NK;
        }
        return null;
    }

    private Klagevurdering hentKlagevurdering(KlageVurderingResultat vurderingsresultat) {
        KlageVurdering klageVurdering = vurderingsresultat.getKlageVurdering();
        KlageVurderingOmgjør klageVurderingOmgjør = vurderingsresultat.getKlageVurderingOmgjør();
        if (KlageVurdering.STADFESTE_YTELSESVEDTAK.equals(klageVurdering)) {
            return Klagevurdering.STADFESTE_YTELSESVEDTAK;
        } else if (KlageVurdering.OPPHEVE_YTELSESVEDTAK.equals(klageVurdering)) {
            return Klagevurdering.OPPHEVE_YTELSESVEDTAK;
        } else if (KlageVurdering.MEDHOLD_I_KLAGE.equals(klageVurdering)) {
            if (KlageVurderingOmgjør.DELVIS_MEDHOLD_I_KLAGE.equals(klageVurderingOmgjør)) {
                return Klagevurdering.DELVIS_MEDHOLD_I_KLAGE;
            }
            if (KlageVurderingOmgjør.UGUNST_MEDHOLD_I_KLAGE.equals(klageVurderingOmgjør)) {
                return Klagevurdering.UGUNST_MEDHOLD_I_KLAGE;
            }
            return Klagevurdering.MEDHOLD_I_KLAGE;
        } else if (KlageVurdering.AVVIS_KLAGE.equals(klageVurdering)) {
            return Klagevurdering.AVVIS_KLAGE;
        } else if (KlageVurdering.HJEMSENDE_UTEN_Å_OPPHEVE.equals(klageVurdering)) {
            return Klagevurdering.HJEMSENDE_UTEN_Å_OPPHEVE;
        }
        return null;
    }

    private KlageMedholdAarsak hentKlageMedholdårsak(KlageVurderingResultat vurderingsresultat) {
        KlageMedholdÅrsak klageMedholdÅrsak = vurderingsresultat.getKlageMedholdÅrsak();
        if (KlageMedholdÅrsak.NYE_OPPLYSNINGER.equals(klageMedholdÅrsak)) {
            return KlageMedholdAarsak.NYE_OPPLYSNINGER;
        } else if (KlageMedholdÅrsak.ULIK_VURDERING.equals(klageMedholdÅrsak)) {
            return KlageMedholdAarsak.ULIK_VURDERING;
        } else if (KlageMedholdÅrsak.ULIK_REGELVERKSTOLKNING.equals(klageMedholdÅrsak)) {
            return KlageMedholdAarsak.ULIK_REGELVERKSTOLKNING;
        } else if (KlageMedholdÅrsak.PROSESSUELL_FEIL.equals(klageMedholdÅrsak)) {
            return KlageMedholdAarsak.PROSESSUELL_FEIL;
        }
        return null;
    }

    private KlageAvvistAarsak hentKlageAvvistårsak(KlageFormkravEntitet klageFormkrav) {
        List<KlageAvvistÅrsak> klageAvvistÅrsak = klageFormkrav.hentAvvistÅrsaker();
        if (klageAvvistÅrsak.contains(KlageAvvistÅrsak.KLAGET_FOR_SENT)) {
            return KlageAvvistAarsak.KLAGET_FOR_SENT;
        } else if (klageAvvistÅrsak.contains(KlageAvvistÅrsak.KLAGE_UGYLDIG)) {
            return KlageAvvistAarsak.KLAGE_UGYLDIG;
        }
        return null;
    }

    private void leggTilAnkeVerdier(Ankevurderingresultat ankeVurderingResultat, Behandling behandling) {
        Optional<AnkeVurderingResultatEntitet> optionalGjeldendeAnkevurderingresultat = ankeRepository.hentAnkeVurderingResultat(behandling.getId());
        if (optionalGjeldendeAnkevurderingresultat.isPresent()) {
            AnkeVurderingResultatEntitet gjeldendeAnkevurderingresultat = optionalGjeldendeAnkevurderingresultat.get();
            ankeVurderingResultat.setAnkeAvvistAarsak(hentAnkeAvvistårsak(optionalGjeldendeAnkevurderingresultat.get()));
            ankeVurderingResultat.setAnkeOmgjoerAarsak(hentAnkeOmgjørårsak(gjeldendeAnkevurderingresultat));
            ankeVurderingResultat.setAnkeVurdering(hentAnkevurdering(gjeldendeAnkevurderingresultat));
        }
    }

    private AnkeAvvistAarsak hentAnkeAvvistårsak(AnkeVurderingResultatEntitet ankeVurderingResultat) {
        List<AnkeAvvistÅrsak> ankeAvvistÅrsak = ankeVurderingResultat.hentAvvistÅrsaker();
        if (ankeAvvistÅrsak.contains(AnkeAvvistÅrsak.ANKE_FOR_SENT)) {
            return AnkeAvvistAarsak.ANKE_FOR_SENT;
        } else if (ankeAvvistÅrsak.contains(AnkeAvvistÅrsak.ANKE_UGYLDIG)) {
            return AnkeAvvistAarsak.ANKE_UGYLDIG;
        }
        return null;
    }

    private AnkeOmgjoerAarsak hentAnkeOmgjørårsak(AnkeVurderingResultatEntitet vurderingsresultat) {
        AnkeOmgjørÅrsak ankeOmgjørÅrsak = vurderingsresultat.getAnkeOmgjørÅrsak();
        if (AnkeOmgjørÅrsak.NYE_OPPLYSNINGER.equals(ankeOmgjørÅrsak)) {
            return AnkeOmgjoerAarsak.NYE_OPPLYSNINGER;
        } else if (AnkeOmgjørÅrsak.ULIK_VURDERING.equals(ankeOmgjørÅrsak)) {
            return AnkeOmgjoerAarsak.ULIK_VURDERING;
        } else if (AnkeOmgjørÅrsak.ULIK_REGELVERKSTOLKNING.equals(ankeOmgjørÅrsak)) {
            return AnkeOmgjoerAarsak.ULIK_REGELVERKSTOLKNING;
        } else if (AnkeOmgjørÅrsak.PROSESSUELL_FEIL.equals(ankeOmgjørÅrsak)) {
            return AnkeOmgjoerAarsak.PROSESSUELL_FEIL;
        }
        return null;
    }

    private Ankevurdering hentAnkevurdering(AnkeVurderingResultatEntitet vurderingsresultat) {
        AnkeVurdering ankeVurdering = vurderingsresultat.getAnkeVurdering();
        AnkeVurderingOmgjør ankeVurderingOmgjør = vurderingsresultat.getAnkeVurderingOmgjør();
        if (AnkeVurdering.ANKE_STADFESTE_YTELSESVEDTAK.equals(ankeVurdering)) {
            return Ankevurdering.ANKE_STADFESTE_YTELSESVEDTAK;
        } else if (AnkeVurdering.ANKE_OMGJOER.equals(ankeVurdering)) {
            if (AnkeVurderingOmgjør.ANKE_DELVIS_OMGJOERING_TIL_GUNST.equals(ankeVurderingOmgjør)) {
                return Ankevurdering.ANKE_DELVIS_OMGJOERING_TIL_GUNST;
            }
            if (AnkeVurderingOmgjør.ANKE_TIL_UGUNST.equals(ankeVurderingOmgjør)) {
                return Ankevurdering.ANKE_TIL_UGUNST;
            }
            return Ankevurdering.ANKE_OMGJOER;
        } else if (AnkeVurdering.ANKE_AVVIS.equals(ankeVurdering)) {
            return Ankevurdering.ANKE_AVVIS;
        } else if (AnkeVurdering.ANKE_OPPHEVE_OG_HJEMSENDE.equals(ankeVurdering)) {
            return Ankevurdering.ANKE_OPPHEVE_OG_HJEMSENDE;
        }
        return null;
    }

    private void setBehandlingsresultatType(Behandlingsresultat behandlingsresultat, Behandling behandling) {
        Optional<BehandlingVedtak> behandlingVedtak = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId());
        if (behandlingVedtak.isPresent()) {
            BehandlingResultatType behandlingResultatType = behandlingVedtak.get().getBehandlingsresultat().getBehandlingResultatType();
            if (BehandlingResultatType.INNVILGET.equals(behandlingResultatType)) {
                behandlingsresultat.setBehandlingsresultat(VedtakXmlUtil.lagKodeverksOpplysning(BehandlingResultatType.INNVILGET));
            } else if (erBehandlingResultatTypeKlageEllerAnke(behandlingResultatType)) {
                behandlingsresultat.setBehandlingsresultat(VedtakXmlUtil.lagKodeverksOpplysning(behandlingResultatType));
            } else {
                behandlingsresultat.setBehandlingsresultat(VedtakXmlUtil.lagKodeverksOpplysning(BehandlingResultatType.AVSLÅTT));
            }
        }
    }

    private boolean erBehandlingResultatTypeKlageEllerAnke(BehandlingResultatType behandlingResultatType) {
        return BehandlingResultatType.getKlageKoder().contains(behandlingResultatType) || BehandlingResultatType.getAnkeKoder().contains(behandlingResultatType);
    }

    private void setManuelleVurderinger(Behandlingsresultat behandlingsresultat, Behandling behandling) {
        Set<Aksjonspunkt> alleAksjonspunkter = behandling.getAksjonspunkter();
        if (!alleAksjonspunkter.isEmpty()) {

            Behandlingsresultat.ManuelleVurderinger manuelleVurderinger = v2ObjectFactory.createBehandlingsresultatManuelleVurderinger();
            alleAksjonspunkter
                .forEach(aksjonspunkt -> leggTilManuellVurdering(manuelleVurderinger, aksjonspunkt));
            behandlingsresultat.setManuelleVurderinger(manuelleVurderinger);
        }
    }

    private void leggTilManuellVurdering(Behandlingsresultat.ManuelleVurderinger manuelleVurderinger, Aksjonspunkt aksjonspunkt) {
        ManuellVurderingsResultat manuellVurderingsResultat = v2ObjectFactory.createManuellVurderingsResultat();
        manuellVurderingsResultat.setAksjonspunkt(VedtakXmlUtil.lagKodeverksOpplysningForAksjonspunkt(aksjonspunkt.getAksjonspunktDefinisjon()));
        if (aksjonspunkt.getAksjonspunktDefinisjon().getVilkårType() != null) {
            manuellVurderingsResultat.setGjelderVilkaar(VedtakXmlUtil.lagKodeverksOpplysning(aksjonspunkt.getAksjonspunktDefinisjon().getVilkårType()));
        }
        if (aksjonspunkt.getBegrunnelse() != null && !aksjonspunkt.getBegrunnelse().isEmpty()) {
            manuellVurderingsResultat.setSaksbehandlersBegrunnelse(aksjonspunkt.getBegrunnelse());
        }
        manuelleVurderinger.getManuellVurdering().add(manuellVurderingsResultat);
    }

    private void setVurderteVilkaar(Behandlingsresultat behandlingsresultat, Behandling behandling) {
        Optional<VilkårResultat> vilkårResultatOpt = vilkårResultatRepository.hentHvisEksisterer(behandling.getId());
        if (vilkårResultatOpt.isPresent()) {
            VilkårResultat vilkårResultat = vilkårResultatOpt.get();
            VurderteVilkaar vurderteVilkaar = v2ObjectFactory.createVurderteVilkaar();
            List<Vilkaar> vilkår = vurderteVilkaar.getVilkaar();
            Comparator<Vilkår> vilkårComparator = Comparator.comparing(Vilkår::getId);
            vilkårResultat.getVilkårene().stream().sorted(vilkårComparator).forEach(vk -> vilkår.add(lagVilkår(vk, behandling)));
            behandlingsresultat.setVurderteVilkaar(vurderteVilkaar);
        }
    }

    private Vilkaar lagVilkår(Vilkår vilkårFraBehandling, Behandling behandling) {
        Vilkaar vilkår = v2ObjectFactory.createVilkaar();
        vilkår.setType(VedtakXmlUtil.lagKodeverksOpplysning(vilkårFraBehandling.getVilkårType()));

        // Sett utfall
        if (VilkårUtfallType.OPPFYLT.equals(vilkårFraBehandling.getGjeldendeVilkårUtfall())) {
            vilkår.setUtfall(Vilkaarsutfall.OPPFYLT);
        } else if (VilkårUtfallType.IKKE_OPPFYLT.equals(vilkårFraBehandling.getGjeldendeVilkårUtfall())) {
            vilkår.setUtfall(Vilkaarsutfall.IKKE_OPPFYLT);
        } else if (VilkårUtfallType.IKKE_VURDERT.equals(vilkårFraBehandling.getGjeldendeVilkårUtfall())) {
            vilkår.setUtfall(Vilkaarsutfall.IKKE_VURDERT);
        }

        if (vilkårFraBehandling.getVilkårUtfallMerknad() != null) {
            KodeverksOpplysning kodeverksOpplysning = new KodeverksOpplysning();
            VilkårUtfallMerknad vilkårUtfallMerknad = vilkårFraBehandling.getVilkårUtfallMerknad();
            kodeverksOpplysning.setKode(vilkårUtfallMerknad.getKode());
            kodeverksOpplysning.setValue(vilkårUtfallMerknad.getNavn());
            kodeverksOpplysning.setKodeverk(vilkårUtfallMerknad.getKodeverk());
            vilkår.setUtfallMerknad(kodeverksOpplysning);
        }
        vilkår.setVurdert(vilkårFraBehandling.erManueltVurdert() ? Vurderingsvariant.MANUELT : Vurderingsvariant.AUTOMATISK);
        var xmlTjeneste = FagsakYtelseTypeRef.Lookup.find(vilkårsgrunnlagXmlTjeneste, behandling.getFagsakYtelseType()).orElseThrow();
        xmlTjeneste.setVilkårsgrunnlag(behandling, vilkårFraBehandling, vilkår);
        return vilkår;
    }

    private void setBehandlingstype(Behandlingsresultat behandlingsresultat, Behandling behandling) {
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            behandlingsresultat.setBehandlingstype(Behandlingstype.KLAGE);
        } else if (BehandlingType.ANKE.equals(behandling.getType())) {
            behandlingsresultat.setBehandlingstype(Behandlingstype.ANKE);
        } else if (BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) {
            behandlingsresultat.setBehandlingstype(Behandlingstype.FOERSTEGANGSSOEKNAD);
        } else if (BehandlingType.REVURDERING.equals(behandling.getType())) {
            behandlingsresultat.setBehandlingstype(Behandlingstype.REVURDERING);
        }
    }
}
