package no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3;

import java.time.LocalDate;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
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

    private static final Logger LOG = LoggerFactory.getLogger(FamilieHendelseOversetter.class);

    private final FamilieHendelseRepository familieHendelseRepository;

    public FamilieHendelseOversetter(FamilieHendelseRepository familieHendelseRepository) {
        this.familieHendelseRepository = familieHendelseRepository;
    }


    void oversettPersisterFamilieHendelse(SøknadWrapper wrapper, Behandling behandling, SøknadEntitet.Builder søknadBuilder) {
        var loggFør = loggFamilieHendelseForFørstegangPåRevurdering(behandling, "FØR");
        var hendelseBuilder = familieHendelseRepository.opprettBuilderFor(behandling.getId());
        if (wrapper.getOmYtelse() instanceof Svangerskapspenger svangerskapspenger) {
            byggFamilieHendelseForSvangerskap(svangerskapspenger, hendelseBuilder);
        } else {
            var soekersRelasjonTilBarnet = getSoekersRelasjonTilBarnet(wrapper);
            if (soekersRelasjonTilBarnet instanceof Foedsel foedsel) {
                byggFødselsrelaterteFelter(foedsel, hendelseBuilder);
            } else if (soekersRelasjonTilBarnet instanceof Termin termin) {
                byggTerminrelaterteFelter(termin, hendelseBuilder);
            } else if (soekersRelasjonTilBarnet instanceof Adopsjon adopsjon) {
                byggAdopsjonsrelaterteFelter(adopsjon, hendelseBuilder);
            } else if (soekersRelasjonTilBarnet instanceof Omsorgsovertakelse omsorgsovertakelse) {
                byggOmsorgsovertakelsesrelaterteFelter(omsorgsovertakelse, hendelseBuilder, søknadBuilder);
            }
        }
        familieHendelseRepository.lagre(behandling.getId(), hendelseBuilder);
        if (loggFør) {
            loggFamilieHendelseForFørstegangPåRevurdering(behandling, "ETTER");
        }
    }

    private boolean loggFamilieHendelseForFørstegangPåRevurdering(Behandling behandling, String infix) {
        var aggregat = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId());
        if (aggregat.isEmpty()) {
            return false;
        }
        var harBekreftetFamiliehendelse = aggregat.flatMap(FamilieHendelseGrunnlagEntitet::getGjeldendeBekreftetVersjon).isPresent();
        var søknadFamilieHendelseType = aggregat.map(FamilieHendelseGrunnlagEntitet::getSøknadVersjon).map(FamilieHendelseEntitet::getType).orElse(FamilieHendelseType.UDEFINERT);
        var gjeldendeFamilieHendelseType = aggregat.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon).map(FamilieHendelseEntitet::getType).orElse(FamilieHendelseType.UDEFINERT);
        LocalDate søknadFamiliehendelseDato = null;
        LocalDate gjeldendeFamiliehendelseDato = null;
        try {
            søknadFamiliehendelseDato = aggregat.map(FamilieHendelseGrunnlagEntitet::getSøknadVersjon)
                .map(FamilieHendelseEntitet::getSkjæringstidspunkt).orElse(null);
            gjeldendeFamiliehendelseDato = aggregat.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
                .map(FamilieHendelseEntitet::getSkjæringstidspunkt).orElse(null);
        } catch (Exception e) {
            // Intentionally empty
        }
        LOG.info("OversettFørstegang {} {}: type {} dato {} gjeldende type {} dato {} for bId {} type {} yt {}", infix,
            harBekreftetFamiliehendelse ? "bekreftet" : "søknad", søknadFamilieHendelseType.getKode(), søknadFamiliehendelseDato,
            gjeldendeFamilieHendelseType.getKode(), gjeldendeFamiliehendelseDato, behandling.getId(), behandling.getType().getKode(),
            behandling.getFagsakYtelseType().getKode());
        return true;
    }

    private void byggFamilieHendelseForSvangerskap(Svangerskapspenger omYtelse,
                                                   FamilieHendelseBuilder hendelseBuilder) {
        var termindato = omYtelse.getTermindato();
        Objects.requireNonNull(termindato, "Termindato må være oppgitt");
        hendelseBuilder.medTerminbekreftelse(hendelseBuilder.getTerminbekreftelseBuilder().medTermindato(termindato));
        var fødselsdato = omYtelse.getFødselsdato();
        if (fødselsdato != null) {
            hendelseBuilder.erFødsel().medFødselsDato(fødselsdato).medAntallBarn(1);
        }
    }

    private void byggFødselsrelaterteFelter(Foedsel fødsel, FamilieHendelseBuilder hendelseBuilder) {
        if (fødsel.getFoedselsdato() == null) {
            throw new IllegalArgumentException("Utviklerfeil: Ved fødsel skal det være eksakt én fødselsdato");
        }

        var fødselsdato = fødsel.getFoedselsdato();
        if (fødsel.getTermindato() != null) {
            hendelseBuilder.medTerminbekreftelse(
                hendelseBuilder.getTerminbekreftelseBuilder().medTermindato(fødsel.getTermindato()));
        }
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

    private void byggOmsorgsovertakelsesrelaterteFelter(Omsorgsovertakelse omsorgsovertakelse,
                                                        FamilieHendelseBuilder hendelseBuilder,
                                                        SøknadEntitet.Builder søknadBuilder) {
        var fødselsdatoene = omsorgsovertakelse.getFoedselsdato();

        hendelseBuilder.tilbakestillBarn().medAntallBarn(omsorgsovertakelse.getAntallBarn());
        var familieHendelseAdopsjon = hendelseBuilder.getAdopsjonBuilder().medOmsorgsovertakelseDato(omsorgsovertakelse.getOmsorgsovertakelsesdato());
        for (var localDate : fødselsdatoene) {
            hendelseBuilder.leggTilBarn(localDate);
        }
        hendelseBuilder.erOmsorgovertagelse();
        hendelseBuilder.medAdopsjon(familieHendelseAdopsjon);

        // Må også settes på søknad
        søknadBuilder.medFarSøkerType(tolkFarSøkerType(omsorgsovertakelse.getOmsorgsovertakelseaarsak()));
    }

    private FarSøkerType tolkFarSøkerType(Omsorgsovertakelseaarsaker omsorgsovertakelseaarsaker) {
        return FarSøkerType.fraKode(omsorgsovertakelseaarsaker.getKode());
    }


    private void byggAdopsjonsrelaterteFelter(Adopsjon adopsjon, FamilieHendelseBuilder hendelseBuilder) {
        var fødselsdatoene = adopsjon.getFoedselsdato();

        hendelseBuilder.tilbakestillBarn().medAntallBarn(adopsjon.getAntallBarn());
        var familieHendelseAdopsjon = hendelseBuilder.getAdopsjonBuilder()
            .medAnkomstDato(adopsjon.getAnkomstdato())
            .medErEktefellesBarn(adopsjon.isAdopsjonAvEktefellesBarn())
            .medOmsorgsovertakelseDato(adopsjon.getOmsorgsovertakelsesdato());
        for (var localDate : fødselsdatoene) {
            hendelseBuilder.leggTilBarn(localDate);
        }
        hendelseBuilder.medAdopsjon(familieHendelseAdopsjon);
    }

    private SoekersRelasjonTilBarnet getSoekersRelasjonTilBarnet(SøknadWrapper skjema) {
        SoekersRelasjonTilBarnet relasjonTilBarnet = null;
        var omYtelse = skjema.getOmYtelse();
        if (omYtelse instanceof Foreldrepenger foreldrepenger) {
            relasjonTilBarnet = foreldrepenger.getRelasjonTilBarnet();
        } else if (omYtelse instanceof Engangsstønad engangsstønad) {
            relasjonTilBarnet = engangsstønad.getSoekersRelasjonTilBarnet();
        }

        Objects.requireNonNull(relasjonTilBarnet, "Relasjon til barnet må være oppgitt");
        return relasjonTilBarnet;
    }

}
