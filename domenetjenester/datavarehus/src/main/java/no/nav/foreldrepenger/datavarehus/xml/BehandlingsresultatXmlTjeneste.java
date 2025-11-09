package no.nav.foreldrepenger.datavarehus.xml;

import java.util.Comparator;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

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
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
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
import no.nav.vedtak.felles.xml.vedtak.v2.ObjectFactory;
import no.nav.vedtak.felles.xml.vedtak.v2.Vedtak;
import no.nav.vedtak.felles.xml.vedtak.v2.Vilkaar;
import no.nav.vedtak.felles.xml.vedtak.v2.Vilkaarsutfall;
import no.nav.vedtak.felles.xml.vedtak.v2.Vurderingsvariant;

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
        var optionalGjeldendeKlagevurderingresultat = klageRepository.hentGjeldendeKlageVurderingResultat(behandling);
        var optionalKlageFormkrav = klageRepository.hentGjeldendeKlageFormkrav(behandling.getId());
        if (optionalGjeldendeKlagevurderingresultat.isPresent() && optionalKlageFormkrav.isPresent()) {
            var gjeldendeKlagevurderingresultat = optionalGjeldendeKlagevurderingresultat.get();
            klagevurderingresultat.setKlageAvvistAarsak(hentKlageAvvistårsak(optionalKlageFormkrav.get()));
            klagevurderingresultat.setKlageMedholdAarsak(hentKlageMedholdårsak(gjeldendeKlagevurderingresultat));
            klagevurderingresultat.setKlageVurdering(hentKlagevurdering(gjeldendeKlagevurderingresultat));
            klagevurderingresultat.setKlageVurdertAv(hentKlageVurdertAv(gjeldendeKlagevurderingresultat));
        }
    }

    private KlageVurdertAv hentKlageVurdertAv(KlageVurderingResultat vurderingsresultat) {
        var klageVurdertAv = vurderingsresultat.getKlageVurdertAv();
        if (no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv.NFP.equals(klageVurdertAv)) {
            return KlageVurdertAv.NFP;
        }
        if (no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv.NK.equals(klageVurdertAv)) {
            return KlageVurdertAv.NK;
        }
        return null;
    }

    private Klagevurdering hentKlagevurdering(KlageVurderingResultat vurderingsresultat) {
        var klageVurdering = vurderingsresultat.getKlageVurdering();
        var klageVurderingOmgjør = vurderingsresultat.getKlageVurderingOmgjør();
        if (KlageVurdering.STADFESTE_YTELSESVEDTAK.equals(klageVurdering)) {
            return Klagevurdering.STADFESTE_YTELSESVEDTAK;
        }
        if (KlageVurdering.OPPHEVE_YTELSESVEDTAK.equals(klageVurdering)) {
            return Klagevurdering.OPPHEVE_YTELSESVEDTAK;
        }
        if (KlageVurdering.MEDHOLD_I_KLAGE.equals(klageVurdering)) {
            if (KlageVurderingOmgjør.DELVIS_MEDHOLD_I_KLAGE.equals(klageVurderingOmgjør)) {
                return Klagevurdering.DELVIS_MEDHOLD_I_KLAGE;
            }
            if (KlageVurderingOmgjør.UGUNST_MEDHOLD_I_KLAGE.equals(klageVurderingOmgjør)) {
                return Klagevurdering.UGUNST_MEDHOLD_I_KLAGE;
            }
            return Klagevurdering.MEDHOLD_I_KLAGE;
        }
        if (KlageVurdering.AVVIS_KLAGE.equals(klageVurdering)) {
            return Klagevurdering.AVVIS_KLAGE;
        }
        if (KlageVurdering.HJEMSENDE_UTEN_Å_OPPHEVE.equals(klageVurdering)) {
            return Klagevurdering.HJEMSENDE_UTEN_Å_OPPHEVE;
        }
        return null;
    }

    private KlageMedholdAarsak hentKlageMedholdårsak(KlageVurderingResultat vurderingsresultat) {
        var klageMedholdÅrsak = vurderingsresultat.getKlageMedholdÅrsak();
        if (KlageMedholdÅrsak.NYE_OPPLYSNINGER.equals(klageMedholdÅrsak)) {
            return KlageMedholdAarsak.NYE_OPPLYSNINGER;
        }
        if (KlageMedholdÅrsak.ULIK_VURDERING.equals(klageMedholdÅrsak)) {
            return KlageMedholdAarsak.ULIK_VURDERING;
        }
        if (KlageMedholdÅrsak.ULIK_REGELVERKSTOLKNING.equals(klageMedholdÅrsak)) {
            return KlageMedholdAarsak.ULIK_REGELVERKSTOLKNING;
        }
        if (KlageMedholdÅrsak.PROSESSUELL_FEIL.equals(klageMedholdÅrsak)) {
            return KlageMedholdAarsak.PROSESSUELL_FEIL;
        }
        return null;
    }

    private KlageAvvistAarsak hentKlageAvvistårsak(KlageFormkravEntitet klageFormkrav) {
        var klageAvvistÅrsak = klageFormkrav.hentAvvistÅrsaker();
        if (klageAvvistÅrsak.contains(KlageAvvistÅrsak.KLAGET_FOR_SENT)) {
            return KlageAvvistAarsak.KLAGET_FOR_SENT;
        }
        if (klageAvvistÅrsak.contains(KlageAvvistÅrsak.KLAGE_UGYLDIG)) {
            return KlageAvvistAarsak.KLAGE_UGYLDIG;
        }
        return null;
    }

    private void leggTilAnkeVerdier(Ankevurderingresultat ankeVurderingResultat, Behandling behandling) {
        ankeRepository.hentAnkeVurderingResultat(behandling.getId()).ifPresent(avr -> {
            ankeVurderingResultat.setAnkeAvvistAarsak(hentAnkeAvvistårsak(avr));
            ankeVurderingResultat.setAnkeOmgjoerAarsak(hentAnkeOmgjørårsak(avr.getAnkeOmgjørÅrsak()));
            ankeVurderingResultat.setAnkeVurdering(hentAnkevurdering(avr));
            ankeVurderingResultat.setTrygdrettVurdering(hentTrygderettvurdering(avr));
            ankeVurderingResultat.setTrygdrettOmgjoerAarsak(hentAnkeOmgjørårsak(avr.getTrygderettOmgjørÅrsak()));
        });
    }

    private AnkeAvvistAarsak hentAnkeAvvistårsak(AnkeVurderingResultatEntitet ankeVurderingResultat) {
        var ankeAvvistÅrsak = ankeVurderingResultat.hentAvvistÅrsaker();
        if (ankeAvvistÅrsak.contains(AnkeAvvistÅrsak.ANKE_FOR_SENT)) {
            return AnkeAvvistAarsak.ANKE_FOR_SENT;
        }
        if (ankeAvvistÅrsak.contains(AnkeAvvistÅrsak.ANKE_UGYLDIG)) {
            return AnkeAvvistAarsak.ANKE_UGYLDIG;
        }
        return null;
    }

    private AnkeOmgjoerAarsak hentAnkeOmgjørårsak(AnkeOmgjørÅrsak ankeOmgjørÅrsak) {
        if (AnkeOmgjørÅrsak.NYE_OPPLYSNINGER.equals(ankeOmgjørÅrsak)) {
            return AnkeOmgjoerAarsak.NYE_OPPLYSNINGER;
        }
        if (AnkeOmgjørÅrsak.ULIK_VURDERING.equals(ankeOmgjørÅrsak)) {
            return AnkeOmgjoerAarsak.ULIK_VURDERING;
        }
        if (AnkeOmgjørÅrsak.ULIK_REGELVERKSTOLKNING.equals(ankeOmgjørÅrsak)) {
            return AnkeOmgjoerAarsak.ULIK_REGELVERKSTOLKNING;
        }
        if (AnkeOmgjørÅrsak.PROSESSUELL_FEIL.equals(ankeOmgjørÅrsak)) {
            return AnkeOmgjoerAarsak.PROSESSUELL_FEIL;
        }
        return null;
    }

    private Ankevurdering hentAnkevurdering(AnkeVurderingResultatEntitet vurderingsresultat) {
        var ankeVurdering = vurderingsresultat.getAnkeVurdering();
        var ankeVurderingOmgjør = vurderingsresultat.getAnkeVurderingOmgjør();
        return getAnkevurdering(ankeVurdering, ankeVurderingOmgjør);
    }

    private Ankevurdering hentTrygderettvurdering(AnkeVurderingResultatEntitet vurderingsresultat) {
        var trettVurdering = vurderingsresultat.getAnkeVurdering();
        var trettVurderingOmgjør = vurderingsresultat.getAnkeVurderingOmgjør();
        return getAnkevurdering(trettVurdering, trettVurderingOmgjør);
    }

    private Ankevurdering getAnkevurdering(AnkeVurdering ankeVurdering, AnkeVurderingOmgjør ankeVurderingOmgjør) {
        if (AnkeVurdering.ANKE_STADFESTE_YTELSESVEDTAK.equals(ankeVurdering)) {
            return Ankevurdering.ANKE_STADFESTE_YTELSESVEDTAK;
        }
        if (AnkeVurdering.ANKE_OMGJOER.equals(ankeVurdering)) {
            if (AnkeVurderingOmgjør.ANKE_DELVIS_OMGJOERING_TIL_GUNST.equals(ankeVurderingOmgjør)) {
                return Ankevurdering.ANKE_DELVIS_OMGJOERING_TIL_GUNST;
            }
            if (AnkeVurderingOmgjør.ANKE_TIL_UGUNST.equals(ankeVurderingOmgjør)) {
                return Ankevurdering.ANKE_TIL_UGUNST;
            }
            return Ankevurdering.ANKE_OMGJOER;
        }
        if (AnkeVurdering.ANKE_AVVIS.equals(ankeVurdering)) {
            return Ankevurdering.ANKE_AVVIS;
        }
        if (AnkeVurdering.ANKE_OPPHEVE_OG_HJEMSENDE.equals(ankeVurdering)) {
            return Ankevurdering.ANKE_OPPHEVE_OG_HJEMSENDE;
        }
        if (AnkeVurdering.ANKE_HJEMSEND_UTEN_OPPHEV.equals(ankeVurdering)) {
            return Ankevurdering.ANKE_HJEMSENDE_UTEN_OPPHEV;
        }
        return null;
    }

    private void setBehandlingsresultatType(Behandlingsresultat behandlingsresultat, Behandling behandling) {
        var behandlingVedtak = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId());
        if (behandlingVedtak.isPresent()) {
            var behandlingResultatType = behandlingVedtak.get().getBehandlingsresultat().getBehandlingResultatType();
            if (BehandlingResultatType.INNVILGET.equals(behandlingResultatType)) {
                behandlingsresultat.setBehandlingsresultat(VedtakXmlUtil.lagKodeverksOpplysning(BehandlingResultatType.INNVILGET));
            } else if (BehandlingType.ANKE.equals(behandling.getType()) || BehandlingType.KLAGE.equals(behandling.getType())) {
                behandlingsresultat.setBehandlingsresultat(VedtakXmlUtil.lagKodeverksOpplysning(behandlingResultatType));
            } else {
                behandlingsresultat.setBehandlingsresultat(VedtakXmlUtil.lagKodeverksOpplysning(BehandlingResultatType.AVSLÅTT));
            }
        }
    }

    private void setManuelleVurderinger(Behandlingsresultat behandlingsresultat, Behandling behandling) {
        var alleAksjonspunkter = behandling.getAksjonspunkter();
        if (!alleAksjonspunkter.isEmpty()) {

            var manuelleVurderinger = v2ObjectFactory.createBehandlingsresultatManuelleVurderinger();
            alleAksjonspunkter
                .forEach(aksjonspunkt -> leggTilManuellVurdering(manuelleVurderinger, aksjonspunkt));
            behandlingsresultat.setManuelleVurderinger(manuelleVurderinger);
        }
    }

    private void leggTilManuellVurdering(Behandlingsresultat.ManuelleVurderinger manuelleVurderinger, Aksjonspunkt aksjonspunkt) {
        var manuellVurderingsResultat = v2ObjectFactory.createManuellVurderingsResultat();
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
        var vilkårResultatOpt = vilkårResultatRepository.hentHvisEksisterer(behandling.getId());
        if (vilkårResultatOpt.isPresent()) {
            var vilkårResultat = vilkårResultatOpt.get();
            var vurderteVilkaar = v2ObjectFactory.createVurderteVilkaar();
            var vilkår = vurderteVilkaar.getVilkaar();
            var vilkårComparator = Comparator.comparing(Vilkår::getId);
            vilkårResultat.getVilkårene().stream().sorted(vilkårComparator).forEach(vk -> vilkår.add(lagVilkår(vk, behandling)));
            behandlingsresultat.setVurderteVilkaar(vurderteVilkaar);
        }
    }

    private Vilkaar lagVilkår(Vilkår vilkårFraBehandling, Behandling behandling) {
        var vilkår = v2ObjectFactory.createVilkaar();
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
            var kodeverksOpplysning = new KodeverksOpplysning();
            var vilkårUtfallMerknad = vilkårFraBehandling.getVilkårUtfallMerknad();
            kodeverksOpplysning.setKode(vilkårUtfallMerknad.getKode());
            kodeverksOpplysning.setValue(vilkårUtfallMerknad.getNavn());
            kodeverksOpplysning.setKodeverk(VilkårUtfallMerknad.KODEVERK);
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
