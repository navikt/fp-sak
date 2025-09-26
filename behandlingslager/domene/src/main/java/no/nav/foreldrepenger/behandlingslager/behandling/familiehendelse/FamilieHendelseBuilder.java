package no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse;

import java.time.LocalDate;
import java.util.Optional;

public class FamilieHendelseBuilder {
    private final FamilieHendelseEntitet hendelse;
    private final HendelseVersjonType type;
    private final boolean oppdaterer;
    private HendelseVersjonType hendelseType;
    private AdopsjonBuilder adopsjonBuilder;
    private TerminbekreftelseBuilder terminbekreftelseBuilder;

    private FamilieHendelseBuilder(FamilieHendelseEntitet hendelse, HendelseVersjonType type, boolean oppdaterer) {
        this.hendelse = hendelse;
        this.type = type;
        this.oppdaterer = oppdaterer;
    }

    static FamilieHendelseBuilder ny(HendelseVersjonType type) {
        return new FamilieHendelseBuilder(new FamilieHendelseEntitet(FamilieHendelseType.UDEFINERT), type, false);
    }

    static FamilieHendelseBuilder oppdatere(FamilieHendelseEntitet oppdatere, HendelseVersjonType type) {
        return new FamilieHendelseBuilder(new FamilieHendelseEntitet(oppdatere), type, true);
    }

    /**
     * Kommer til å endre scope til package private
     *
     * @param oppdatere entiteten som skal oppdateres
     * @param type      HendelseVersjonType
     * @return buildern
     */
    public static FamilieHendelseBuilder oppdatere(Optional<FamilieHendelseEntitet> oppdatere, HendelseVersjonType type) {
        return oppdatere.map(oppdatere1 -> oppdatere(oppdatere1, type)).orElseGet(() -> ny(type));
    }

    public FamilieHendelseBuilder leggTilBarn(LocalDate fødselsDato) {
        hendelse.leggTilBarn(new UidentifisertBarnEntitet(fødselsDato, hendelse.getBarna().size() + 1));
        return this;
    }

    public FamilieHendelseBuilder leggTilBarn(LocalDate fødselsDato, LocalDate dødsdato) {
        hendelse.leggTilBarn(new UidentifisertBarnEntitet(hendelse.getBarna().size() + 1, fødselsDato, dødsdato));
        return this;
    }

    public FamilieHendelseBuilder leggTilBarn(UidentifisertBarn barn) {
        var barnEntitet = new UidentifisertBarnEntitet(barn);
        hendelse.leggTilBarn(barnEntitet);
        return this;
    }

    public FamilieHendelseBuilder medFødselsDato(LocalDate fødselsDato) {
        tilbakestillBarn();
        leggTilBarn(fødselsDato);
        return this;
    }

    // Kall evt tilbakestill før du kaller denne
    public FamilieHendelseBuilder medFødselsDato(LocalDate fødselsDato, int antall) {
        if (antall > 0) {
            leggTilBarn(fødselsDato);
            return medFødselsDato(fødselsDato, antall - 1);
        }
        return this;
    }

    public FamilieHendelseBuilder tilbakestillBarn() {
        hendelse.clearBarn();
        return this;
    }

    public FamilieHendelseBuilder medAntallBarn(Integer antallBarn) {
        hendelse.setAntallBarn(antallBarn);
        return this;
    }

    public FamilieHendelseBuilder medErMorForSykVedFødsel(Boolean erMorForSykVedFødsel) {
        hendelse.setMorForSykVedFødsel(erMorForSykVedFødsel);
        return this;
    }

    public FamilieHendelseBuilder medTerminbekreftelse(TerminbekreftelseBuilder terminbekreftelse) {
        if (hendelse.getTerminbekreftelse().isPresent() == terminbekreftelse.getErOppdatering()) {
            hendelse.setTerminbekreftelse(terminbekreftelse.build());
            terminbekreftelseBuilder = null;
            return this;
        }
        throw FamilieHendelseFeil.måBasereSegPåEksisterendeVersjon();
    }

    public FamilieHendelseBuilder medAdopsjon(AdopsjonBuilder adopsjon) {
        if (hendelse.getAdopsjon().isPresent() == adopsjon.getErOppdatering()) {
            hendelse.setAdopsjon(adopsjon.build());
            adopsjonBuilder = null;
            return this;
        }
        throw FamilieHendelseFeil.måBasereSegPåEksisterendeVersjon();
    }

    /**
     * Gjør det mulig å sette type til omsorgovertagelse.
     *
     * @return builder
     */
    public FamilieHendelseBuilder erOmsorgovertagelse() {
        if (hendelse.getType().equals(FamilieHendelseType.UDEFINERT) || hendelse.getType().equals(FamilieHendelseType.OMSORG)) {
            hendelse.setType(FamilieHendelseType.OMSORG);
        } else {
            throw FamilieHendelseFeil.kanIkkeEndreTypePåHendelseFraTil(hendelse.getType(), FamilieHendelseType.OMSORG);
        }
        return this;
    }

    public FamilieHendelseBuilder medFødselType() {
        return medType(FamilieHendelseType.FØDSEL);
    }

    public FamilieHendelseBuilder medTerminType() {
        return medType(FamilieHendelseType.TERMIN);
    }

    private FamilieHendelseBuilder medType(FamilieHendelseType type) {
        if (hendelse.getType().equals(FamilieHendelseType.UDEFINERT) || hendelse.getType().equals(FamilieHendelseType.FØDSEL) || hendelse.getType()
            .equals(FamilieHendelseType.TERMIN)) {
            hendelse.setType(type);
        } else {
            throw FamilieHendelseFeil.kanIkkeEndreTypePåHendelseFraTil(hendelse.getType(), type);
        }
        return this;
    }

    public TerminbekreftelseBuilder getTerminbekreftelseBuilder() {
        if (terminbekreftelseBuilder == null) {
            terminbekreftelseBuilder = TerminbekreftelseBuilder.oppdatere(hendelse.getTerminbekreftelse());
        }
        return terminbekreftelseBuilder;
    }

    public AdopsjonBuilder getAdopsjonBuilder() {
        if (adopsjonBuilder == null) {
            adopsjonBuilder = AdopsjonBuilder.oppdatere(hendelse.getAdopsjon());
        }
        return adopsjonBuilder;
    }

    boolean getErOppdatering() {
        return this.oppdaterer;
    }

    public FamilieHendelseEntitet build() {
        if (hendelse.getTerminbekreftelse().isPresent() && hendelse.getAdopsjon().isPresent()) {
            throw new IllegalStateException("Utviklerfeil: Kan ikke både ha terminbekreftelse og adopsjon");
        }
        if (hendelse.getAdopsjon().isPresent()) {
            if (hendelse.getAdopsjon().get().getOmsorgovertakelseVilkår().equals(OmsorgsovertakelseVilkårType.UDEFINERT)
                && !erSøknadEllerBekreftetVersjonOgSattTilOmsorg()) {
                hendelse.setType(FamilieHendelseType.ADOPSJON);
            } else {
                hendelse.setType(FamilieHendelseType.OMSORG);
            }
        } else if (!hendelse.getBarna().isEmpty() || erHendelsenSattTil(FamilieHendelseType.FØDSEL)) {
            hendelse.setType(FamilieHendelseType.FØDSEL);
        } else if (hendelse.getTerminbekreftelse().isPresent()) {
            hendelse.setType(FamilieHendelseType.TERMIN);
        }
        if (hendelse.getAntallBarn() == null) {
            hendelse.setAntallBarn(hendelse.getBarna().size());
        }
        if (!FamilieHendelseType.TERMIN.equals(hendelse.getType()) && hendelse.getAntallBarn() != hendelse.getBarna().size()) {
            throw new IllegalStateException("Utviklerfeil: Avvik mellom antall barn og uidentifisert barn");
        }
        return hendelse;
    }

    private boolean erSøknadEllerBekreftetVersjonOgSattTilOmsorg() {
        return (type.equals(HendelseVersjonType.SØKNAD) || type.equals(HendelseVersjonType.BEKREFTET)) && hendelse.getType() != null
            && hendelse.getType().equals(FamilieHendelseType.OMSORG);
    }

    private boolean erHendelsenSattTil(FamilieHendelseType type) {
        return hendelse.getType() != null && hendelse.getType().equals(type);
    }

    HendelseVersjonType getType() {
        if (hendelseType != null && !hendelseType.equals(type)) {
            return hendelseType;
        }
        return type;
    }

    Integer getAntallBarn() {
        return this.hendelse.getAntallBarn() != null ? this.hendelse.getAntallBarn() : this.hendelse.getBarna().size();
    }

    void setHendelseType(HendelseVersjonType hendelseType) {
        this.hendelseType = hendelseType;
    }

    HendelseVersjonType getOpprinneligType() {
        return type;
    }

    public static class TerminbekreftelseBuilder {
        private final TerminbekreftelseEntitet kladd;
        private final boolean oppdatering;

        private TerminbekreftelseBuilder(TerminbekreftelseEntitet terminbekreftelse, boolean oppdatering) {
            this.kladd = terminbekreftelse;
            this.oppdatering = oppdatering;
        }

        static TerminbekreftelseBuilder ny() {
            return new TerminbekreftelseBuilder(new TerminbekreftelseEntitet(), false);
        }

        static TerminbekreftelseBuilder oppdatere(TerminbekreftelseEntitet oppdatere) {
            return new TerminbekreftelseBuilder(new TerminbekreftelseEntitet(oppdatere), true);
        }

        static TerminbekreftelseBuilder oppdatere(Optional<TerminbekreftelseEntitet> oppdatere) {
            return oppdatere.map(TerminbekreftelseBuilder::oppdatere).orElseGet(TerminbekreftelseBuilder::ny);
        }

        public TerminbekreftelseBuilder medTermindato(LocalDate termindato) {
            this.kladd.setTermindato(termindato);
            return this;
        }

        public TerminbekreftelseBuilder medUtstedtDato(LocalDate utstedtdato) {
            this.kladd.setUtstedtdato(utstedtdato);
            return this;
        }


        public TerminbekreftelseBuilder medNavnPå(String navn) {
            this.kladd.setNavn(navn);
            return this;
        }

        TerminbekreftelseEntitet build() {
            if (kladd.hasValues()) {
                return kladd;
            }
            throw new IllegalStateException();
        }

        boolean getErOppdatering() {
            return oppdatering;
        }
    }

    public static class AdopsjonBuilder {
        private final AdopsjonEntitet kladd;
        private final boolean oppdatering;

        private AdopsjonBuilder(AdopsjonEntitet adopsjon, boolean oppdatering) {
            this.kladd = adopsjon;
            this.oppdatering = oppdatering;
        }

        static AdopsjonBuilder ny() {
            return new AdopsjonBuilder(new AdopsjonEntitet(), false);
        }

        static AdopsjonBuilder oppdatere(AdopsjonEntitet oppdatere) {
            return new AdopsjonBuilder(new AdopsjonEntitet(oppdatere), true);
        }

        static AdopsjonBuilder oppdatere(Optional<AdopsjonEntitet> oppdatere) {
            return oppdatere.map(AdopsjonBuilder::oppdatere).orElseGet(AdopsjonBuilder::ny);
        }

        public AdopsjonBuilder medAdoptererAlene(boolean adoptererAlene) {
            this.kladd.setAdoptererAlene(adoptererAlene);
            return this;
        }

        public AdopsjonBuilder medErEktefellesBarn(boolean erEktefellesBarn) {
            this.kladd.setErEktefellesBarn(erEktefellesBarn);
            return this;
        }

        public AdopsjonBuilder medOmsorgsovertakelseDato(LocalDate omsorgsovertakelseDato) {
            this.kladd.setOmsorgsovertakelseDato(omsorgsovertakelseDato);
            return this;
        }

        public AdopsjonBuilder medAnkomstDato(LocalDate ankomstDato) {
            this.kladd.setAnkomstNorgeDato(ankomstDato);
            return this;
        }

        public AdopsjonBuilder medForeldreansvarDato(LocalDate foreldreansvarDato) {
            this.kladd.setForeldreansvarDato(foreldreansvarDato);
            return this;
        }


        public AdopsjonBuilder medOmsorgovertalseVilkårType(OmsorgsovertakelseVilkårType vilkårType) {
            this.kladd.setOmsorgsovertakelseVilkårType(vilkårType);
            return this;
        }
        public AdopsjonBuilder medVilkårHjemmel(VilkårHjemmel vilkårHjemmel) {
            this.kladd.setVilkårHjemmel(vilkårHjemmel);
            return this;
        }

        AdopsjonEntitet build() {
            return kladd;
        }

        boolean getErOppdatering() {
            return oppdatering;
        }
    }
}
