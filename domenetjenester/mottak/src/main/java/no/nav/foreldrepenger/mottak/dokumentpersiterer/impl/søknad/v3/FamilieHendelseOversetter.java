package no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.omsorgsovertakelse.OmsorgsovertakelseVilkårTypeUtleder;
import no.nav.vedtak.felles.xml.soeknad.engangsstoenad.v3.Engangsstønad;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Adopsjon;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Foedsel;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Omsorgsovertakelse;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.SoekersRelasjonTilBarnet;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Termin;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.Foreldrepenger;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.Omsorgsovertakelseaarsaker;
import no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Svangerskapspenger;

public class FamilieHendelseOversetter  {

    private final FamilieHendelseRepository familieHendelseRepository;

    public FamilieHendelseOversetter(FamilieHendelseRepository familieHendelseRepository) {
        this.familieHendelseRepository = familieHendelseRepository;
    }

    /**
     * Rolle: Lagre førstegangssøknad for alle ytelser.
     * Dersom behandlingen er førstegangsbehandling så er alt OK. Ditto hvis det mangler familieHendelseGrunnlag
     *
     * Det kan hende at det kommer inn ny førstegangssøknad i en revurdering. Det gjøres da en diffsjekk ved lagring:
     * - Dersom ny søknad er identisk med eksisterende søknadshendelse blir det ikke lagret noe, alt beholdes
     * - Dersom ny søknad er ulik eksisterende søknadshendelse blir lagret grunnlag uten bekreftet/overstyrt
     * - Dersom søknad for Fødsel mangler termindato, så hentes den fra evt gjeldende terminbekreftelse
     */
    void oversettPersisterFamilieHendelse(SøknadWrapper wrapper, Behandling behandling, SøknadEntitet.Builder søknadBuilder) {
        var hendelseBuilder = familieHendelseRepository.opprettBuilderForSøknad(behandling.getId());
        if (wrapper.getOmYtelse() instanceof Svangerskapspenger svangerskapspenger) {
            byggFamilieHendelseForSvangerskap(svangerskapspenger, hendelseBuilder);
        } else {
            var soekersRelasjonTilBarnet = getSoekersRelasjonTilBarnet(wrapper);
            switch (soekersRelasjonTilBarnet) {
                case Foedsel foedsel -> {
                    var gjeldendeTermin = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())
                        .flatMap(FamilieHendelseGrunnlagEntitet::getGjeldendeTerminbekreftelse);
                    byggFødselsrelaterteFelter(foedsel, hendelseBuilder, gjeldendeTermin);
                }
                case Termin termin -> byggTerminrelaterteFelter(termin, hendelseBuilder);
                case Adopsjon adopsjon -> byggAdopsjonsrelaterteFelter(behandling, adopsjon, hendelseBuilder);
                case Omsorgsovertakelse omsorgsovertakelse -> byggOmsorgsovertakelsesrelaterteFelter(behandling, omsorgsovertakelse, hendelseBuilder, søknadBuilder);
                default -> throw new IllegalArgumentException("Ukjent subklasse av SoekersRelasjonTilBarnet: " + soekersRelasjonTilBarnet.getClass().getSimpleName());
            }
        }
        familieHendelseRepository.lagreSøknadHendelse(behandling.getId(), hendelseBuilder);
    }

    private void byggFamilieHendelseForSvangerskap(Svangerskapspenger omYtelse,
                                                   FamilieHendelseBuilder hendelseBuilder) {
        var termindato = omYtelse.getTermindato();
        Objects.requireNonNull(termindato, "Termindato må være oppgitt");
        hendelseBuilder.medTerminbekreftelse(hendelseBuilder.getTerminbekreftelseBuilder().medTermindato(termindato));
        var fødselsdato = omYtelse.getFødselsdato();
        if (fødselsdato != null) {
            hendelseBuilder.medFødselType().medFødselsDato(fødselsdato).medAntallBarn(1);
        }
    }

    private void byggFødselsrelaterteFelter(Foedsel fødsel, FamilieHendelseBuilder hendelseBuilder,
                                            Optional<TerminbekreftelseEntitet> gjeldendeTermin) {
        if (fødsel.getFoedselsdato() == null) {
            throw new IllegalArgumentException("Utviklerfeil: Ved fødsel skal det være eksakt én fødselsdato");
        }

        var fødselsdato = fødsel.getFoedselsdato();
        Optional.ofNullable(fødsel.getTermindato())
            .or(() -> gjeldendeTermin.map(TerminbekreftelseEntitet::getTermindato))
            .ifPresent(d -> {
                var terminBuilder = hendelseBuilder.getTerminbekreftelseBuilder().medTermindato(d);
                hendelseBuilder.medTerminbekreftelse(terminBuilder);
            });
        var antallBarn = fødsel.getAntallBarn();
        hendelseBuilder.tilbakestillBarn().medAntallBarn(antallBarn);
        for (var i = 1; i <= antallBarn; i++) {
            hendelseBuilder.leggTilBarn(fødselsdato);
        }
    }

    private void byggTerminrelaterteFelter(Termin termin, FamilieHendelseBuilder hendelseBuilder) {
        Objects.requireNonNull(termin.getTermindato(), "Termindato må være oppgitt");

        hendelseBuilder.medAntallBarn(termin.getAntallBarn());
        hendelseBuilder.medTerminbekreftelse(hendelseBuilder.getTerminbekreftelseBuilder()
            .medTermindato(termin.getTermindato())
            .medUtstedtDato(termin.getUtstedtdato()));
    }

    private void byggOmsorgsovertakelsesrelaterteFelter(Behandling behandling,
                                                        Omsorgsovertakelse omsorgsovertakelse,
                                                        FamilieHendelseBuilder hendelseBuilder,
                                                        SøknadEntitet.Builder søknadBuilder) {
        var fødselsdatoene = omsorgsovertakelse.getFoedselsdato().stream().sorted(Comparator.naturalOrder()).toList();
        var farSøkerType = tolkFarSøkerType(omsorgsovertakelse.getOmsorgsovertakelseaarsak());

        var utledetDelvilkår = OmsorgsovertakelseVilkårTypeUtleder.utledDelvilkårForeldreansvar(behandling.getFagsakYtelseType(), farSøkerType);
        var delvilkår = Optional.ofNullable(utledetDelvilkår).orElse(OmsorgsovertakelseVilkårType.UDEFINERT);
        hendelseBuilder.tilbakestillBarn().medAntallBarn(omsorgsovertakelse.getAntallBarn());
        var familieHendelseAdopsjon = hendelseBuilder.getAdopsjonBuilder()
            .medOmsorgovertalseVilkårType(delvilkår)
            .medOmsorgsovertakelseDato(omsorgsovertakelse.getOmsorgsovertakelsesdato());
        for (var localDate : fødselsdatoene) {
            hendelseBuilder.leggTilBarn(localDate);
        }
        hendelseBuilder.erOmsorgovertagelse();
        hendelseBuilder.medAdopsjon(familieHendelseAdopsjon);

        // Må også settes på søknad
        søknadBuilder.medFarSøkerType(farSøkerType);
    }

    private FarSøkerType tolkFarSøkerType(Omsorgsovertakelseaarsaker omsorgsovertakelseaarsaker) {
        return FarSøkerType.fraKode(omsorgsovertakelseaarsaker.getKode());
    }


    private void byggAdopsjonsrelaterteFelter(Behandling behandling, Adopsjon adopsjon, FamilieHendelseBuilder hendelseBuilder) {
        var fødselsdatoene = adopsjon.getFoedselsdato().stream().sorted(Comparator.naturalOrder()).toList();

        var utledetDelvilkår = OmsorgsovertakelseVilkårTypeUtleder.utledDelvilkårAdopsjon(behandling.getFagsakYtelseType(), adopsjon.isAdopsjonAvEktefellesBarn());
        var delvilkår = Optional.ofNullable(utledetDelvilkår).orElse(OmsorgsovertakelseVilkårType.UDEFINERT);

        hendelseBuilder.tilbakestillBarn().medAntallBarn(adopsjon.getAntallBarn());
        var familieHendelseAdopsjon = hendelseBuilder.getAdopsjonBuilder()
            .medOmsorgovertalseVilkårType(delvilkår)
            .medAnkomstDato(adopsjon.getAnkomstdato())
            .medErEktefellesBarn(adopsjon.isAdopsjonAvEktefellesBarn())
            .medOmsorgsovertakelseDato(adopsjon.getOmsorgsovertakelsesdato());
        for (var localDate : fødselsdatoene) {
            hendelseBuilder.leggTilBarn(localDate);
        }
        hendelseBuilder.medAdopsjon(familieHendelseAdopsjon);
    }

    private SoekersRelasjonTilBarnet getSoekersRelasjonTilBarnet(SøknadWrapper skjema) {
        return switch (skjema.getOmYtelse()) {
            case Foreldrepenger foreldrepenger -> foreldrepenger.getRelasjonTilBarnet();
            case Engangsstønad engangsstønad -> engangsstønad.getSoekersRelasjonTilBarnet();
            default -> throw new IllegalStateException("Relasjon til barnet må være oppgitt");
        };
    }


}
