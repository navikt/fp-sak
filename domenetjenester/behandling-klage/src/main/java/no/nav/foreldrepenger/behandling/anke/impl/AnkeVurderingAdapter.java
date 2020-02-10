package no.nav.foreldrepenger.behandling.anke.impl;

import java.util.Optional;

public class AnkeVurderingAdapter {
    private String ankeVurderingKode;
    private String begrunnelse;
    private String fritekstTilBrev;
    private String ankeOmgjoerArsakKode;
    private String ankeVurderingOmgjoer;
    private boolean erSubsidiartRealitetsbehandles;
    private boolean erGodkjentAvMedunderskriver;
    private boolean erAnkerIkkePart;
    private boolean erFristIkkeOverholdt;
    private boolean erIkkeKonkret;
    private boolean erIkkeSignert;
    private Long paaAnketBehandlingId;
    private String merknaderFraBruker;
    private boolean erMerknaderMottatt;

    private AnkeVurderingAdapter() {
    }

    public static class Builder {
        private AnkeVurderingAdapter ankeVurderingAdapterMal;

        public Builder() {
            ankeVurderingAdapterMal = new AnkeVurderingAdapter();
        }

        public AnkeVurderingAdapter.Builder medAnkeVurderingKode(String ankeVurderingKode) {
            ankeVurderingAdapterMal.ankeVurderingKode = ankeVurderingKode;
            return this;
        }

        public AnkeVurderingAdapter.Builder medBegrunnelse(String begrunnelse) {
            ankeVurderingAdapterMal.begrunnelse = begrunnelse;
            return this;
        }

        public AnkeVurderingAdapter.Builder medFritekstTilBrev(String fritekstTilBrev) {
            ankeVurderingAdapterMal.fritekstTilBrev = fritekstTilBrev;
            return this;
        }

        public AnkeVurderingAdapter.Builder medAnkeOmgjoerArsakKode(String ankeOmgjoerArsakKode) {
            ankeVurderingAdapterMal.ankeOmgjoerArsakKode = ankeOmgjoerArsakKode;
            return this;
        }

        public AnkeVurderingAdapter.Builder medAnkeVurderingOmgjoer(String ankeVurderingOmgjoer) {
            ankeVurderingAdapterMal.ankeVurderingOmgjoer = ankeVurderingOmgjoer;
            return this;
        }

        public AnkeVurderingAdapter.Builder medErSubsidiartRealitetsbehandles(boolean erSubsidiartRealitetsbehandles) {
            ankeVurderingAdapterMal.erSubsidiartRealitetsbehandles = erSubsidiartRealitetsbehandles;
            return this;
        }

        public AnkeVurderingAdapter.Builder medErGodkjentAvMedunderskriver(boolean erGodkjentAvMedunderskriver) {
            ankeVurderingAdapterMal.erGodkjentAvMedunderskriver = erGodkjentAvMedunderskriver;
            return this;
        }

        public AnkeVurderingAdapter.Builder medErAnkerIkkePart(boolean erAnkerIkkePart) {
            ankeVurderingAdapterMal.erAnkerIkkePart = erAnkerIkkePart;
            return this;
        }

        public AnkeVurderingAdapter.Builder medErFristIkkeOverholdt(boolean erFristIkkeOverholdt) {
            ankeVurderingAdapterMal.erFristIkkeOverholdt = erFristIkkeOverholdt;
            return this;
        }

        public AnkeVurderingAdapter.Builder medErIkkeKonkret(boolean erIkkeKonkret) {
            ankeVurderingAdapterMal.erIkkeKonkret = erIkkeKonkret;
            return this;
        }

        public AnkeVurderingAdapter.Builder medErIkkeSignert(boolean erIkkeSignert) {
            ankeVurderingAdapterMal.erIkkeSignert = erIkkeSignert;
            return this;
        }

        public AnkeVurderingAdapter.Builder medPaaAnketBehandlingId(Long paaAnketBehandlingId) {
            ankeVurderingAdapterMal.paaAnketBehandlingId = paaAnketBehandlingId;
            return this;
        }

        public AnkeVurderingAdapter.Builder medMerknaderFraBruker(String merknaderFraBruker) {
            ankeVurderingAdapterMal.merknaderFraBruker = merknaderFraBruker;
            return this;
        }

        public AnkeVurderingAdapter.Builder medErMerknaderMottatt(boolean erMerknaderMottatt) {
            ankeVurderingAdapterMal.erMerknaderMottatt = erMerknaderMottatt;
            return this;
        }

        public AnkeVurderingAdapter build() {
            return ankeVurderingAdapterMal;
        }
    }

    public String getAnkeVurderingKode() {
        return ankeVurderingKode;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public String getFritekstTilBrev() {
        return fritekstTilBrev;
    }

    public Optional<String> getAnkeOmgjoerArsakKode() {
        return Optional.ofNullable(ankeOmgjoerArsakKode);
    }

    public Optional<String> getAnkeVurderingOmgjoer() {
        return Optional.ofNullable(ankeVurderingOmgjoer);
    }

    public boolean getErSubsidiartRealitetsbehandles() {
        return erSubsidiartRealitetsbehandles;
    }

    public boolean getErGodkjentAvMedunderskriver() {
        return erGodkjentAvMedunderskriver;
    }

    public boolean erAnkerIkkePart() {
        return erAnkerIkkePart;
    }

    public boolean erFristIkkeOverholdt() {
        return erFristIkkeOverholdt;
    }

    public boolean erIkkeKonkret() {
        return erIkkeKonkret;
    }

    public boolean erIkkeSignert() {
        return erIkkeSignert;
    }

    public Long getPaaAnketBehandlingId() {
        return paaAnketBehandlingId;
    }

    public String getMerknaderFraBruker() {
        return merknaderFraBruker;
    }

    public boolean erMerknaderMottatt() {
        return erMerknaderMottatt;
    }
}
