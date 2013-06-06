<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:java="java">


	<xsl:output method="xml" version="1.0" encoding="UTF-8"
		standalone="yes" indent="yes" />


	<!-- Global Variables -->
	<xsl:variable name="beamline" select="tomodb2/acquisition/beamline" />
	<xsl:variable name="path" select="tomodb2/acquisition/disk" />
	<xsl:variable name="nameExp" select="tomodb2/acquisition/nameExp" />
	<!-- select="substring-before( substring-after($path, 'visitor/'), '/')" 
		/> -->
	<!-- Parse the path and get the experiment name. -->

	<xsl:variable name="date" select="tomodb2/acquisition/date" />
	<xsl:variable name="dataset" select="tomodb2/acquisition/scanName" />
	<xsl:variable name="invNamePrefix" select="concat($nameExp, ' ',$beamline)" />

	<!-- HOW does those variable work for multiple acquisitions?? -->



	<!-- ======================================================== -->
	<!-- MAIN template -->
	<xsl:template match="/">

		<xsl:element name="icatdata"  >

			<config>
				<haltOnError>false</haltOnError>
				<localIdRange>
					<min></min>
					<max>0</max>
				</localIdRange>

				<searchids>
					<search>
						<id>-1</id>
						<query>Investigation [id = placeholder]</query>
					</search>
					<search>
						<id>-3</id>
						<query>DatafileFormat [name = 'EDF']</query>
					</search>
				</searchids>
			</config>
			<xsl:apply-templates />
		</xsl:element>
	</xsl:template>



	<!-- ======================================================== -->
	<!-- an "acquisition" = ICAT dataset -->
	<xsl:template match="acquisition">

		<!-- TEST to exclude in-house experiment IF ExpName == Bimline -->
		<xsl:if test="$nameExp != $beamline">  <!-- TO CHAMGE to '=' NOT '!=' -->
		
			<!-- Pull off the Experiment Name -->
			<xsl:element name="ExperimentName">
				<xsl:value-of select="$nameExp" />
			</xsl:element>
			
			<!-- Pull off the Instrument Name -->
			<xsl:element name="Instrument">
				<xsl:value-of select="$beamline" />
			</xsl:element>
			
			<!-- Pull off the DataSet Name -->
			<xsl:element name="DataSetName">
				<xsl:value-of select="$dataset" />
			</xsl:element>

			<xsl:apply-templates />
		</xsl:if>
		<!-- in in-house: DO NOTHING -->

	</xsl:template>

	<!-- ======================================================== -->

	<!-- ++++++++++++++++++++++++++++++++++++++++++++++++ -->

	<xsl:template match="scanName">

		<xsl:element name="dataset">

			<id>-2</id>
			<xsl:variable name="dataset" select="." />

			<xsl:element name="name">
				<xsl:value-of select="$dataset" />
			</xsl:element>

			<xsl:element name="location">
				<xsl:value-of select="$path" />
			</xsl:element>

			<xsl:element name="startDate">
				<xsl:value-of select="$date" />
			</xsl:element>

			<xsl:element name="complete">
				true
			</xsl:element>

			<xsl:element name="investigation">
				<id>-1</id>
			</xsl:element>

			<xsl:element name="type"> <!-- hardcoded dataset TYPE -->
				<xsl:element name="searchId">DatasetType [name = 'acquisition']</xsl:element>
			</xsl:element>
		</xsl:element>
	</xsl:template>


	<!-- Create a template per each ICAT Parameters -->

	<!-- ++++++++++++++++++++++++++++++++++++++++++++++++ -->
	<!-- investigationParameter -->
	<xsl:template match="machineMode|sourceSampleDistance|cameraName">

		<xsl:element name="investigationParameter">

			<xsl:element name="stringValue"> <!-- stringValue OR numericValue OR rangeTop/rangeBottom -->
				<xsl:value-of select="." /> <!-- "." means this element. -->
			</xsl:element>

			<xsl:element name="type">
				<xsl:element name="searchId">ParameterType [name = '<xsl:value-of select="name()" />']</xsl:element>
			</xsl:element>

			<xsl:element name="investigation">
				<id>-1</id>
			</xsl:element>

		</xsl:element>
	</xsl:template>
	<!-- end Parameter -->


	<!-- ++++++++++++++++++++++++++++++++++++++++++++++++ -->
	<!-- DataSet Parameter -->
	<xsl:template
		match="machineCurrentStart|machineCurrentStop|insertionDeviceGap|filter|
	energy|tomo_N|ref_On|ref_N|dark_N|y_Step|ccdtime|scanDuration|distance|scanRange|
	realFinalAngles|cameraFibers|pixelSize">

		<xsl:if test="normalize-space(.)"> <!-- If EMPTY do nothing -->
			<xsl:element name="datasetParameter">

				<xsl:element name="numericValue"> <!-- stringValue OR dateTimeValue OR rangeTop/rangeBottom -->
					<xsl:value-of select="." />
				</xsl:element>

				<xsl:element name="type">
					<xsl:element name="searchId">ParameterType [name = '<xsl:value-of select="name()" />']</xsl:element>
				</xsl:element>

				<xsl:element name="dataset">
					<id>-2</id>
				</xsl:element>

			</xsl:element>
		</xsl:if>
	</xsl:template>
	<!-- end Parameter -->


	<!-- ++++++++++++++++++++++++++++++++++++++++++++++++ -->
	<!-- DataSet Parameter -->
	<xsl:template
		match="insertionDeviceName|filter|monochromatorName|scanType|opticsName|scintillator|
	cameraBinning|ccdMode|ccdstatus">

		<xsl:if test="normalize-space(.)"> <!-- If EMPTY do nothing -->

			<xsl:element name="datasetParameter">

				<xsl:element name="stringValue"> <!-- stringValue OR dateTimeValue OR rangeTop/rangeBottom -->
					<xsl:value-of select="." />
				</xsl:element>

				<xsl:element name="type">
					<xsl:element name="searchId">ParameterType [name = '<xsl:value-of select="name()" />']</xsl:element>
				</xsl:element>

				<xsl:element name="dataset">
					<id>-2</id>
				</xsl:element>


			</xsl:element>
		</xsl:if>
	</xsl:template>
	<!-- end Parameter -->

	<!-- ++++++++++++++++++++++++++++++++++++++++++++++++ -->
	<xsl:template match="projectionSize/*">
		<xsl:element name="datasetParameter">

			<xsl:element name="numericValue"> <!-- stringValue OR dateTimeValue OR rangeTop/rangeBottom -->
				<xsl:value-of select="." />
			</xsl:element>

			<xsl:element name="type">
				<xsl:element name="searchId">ParameterType [name = '<xsl:value-of select="concat('projectionSize_', name())" />']</xsl:element>
			</xsl:element>

			<xsl:element name="dataset">
				<id>-2</id>
			</xsl:element>

		</xsl:element>
	</xsl:template>


	<!-- ++++++++++++++++++++++++++++++++++++++++++++++++ -->
	<xsl:template match="listSlits/slits">

		<!-- Select slit name property -->
		<xsl:variable name="slitType" select="@*" />

		<xsl:for-each select="*">
			<xsl:element name="datasetParameter">

				<xsl:element name="numericValue"> <!-- stringValue OR dateTimeValue OR rangeTop/rangeBottom -->
					<xsl:value-of select="." />
				</xsl:element>

				<xsl:element name="type">
					<xsl:element name="searchId">ParameterType [name = '<xsl:value-of select="concat('slit_', $slitType, '_', name())" />']</xsl:element>
				</xsl:element>

				<xsl:element name="dataset">
					<id>-2</id>
				</xsl:element>

			</xsl:element>
		</xsl:for-each>

	</xsl:template>

	<!-- ++++++++++++++++++++++++++++++++++++++++++++++++ -->
	<xsl:template match="listMotors/motor">

		<!-- Select slit name property -->
		<xsl:variable name="slitType" select="motorPosition/@*" />


		<xsl:element name="datasetParameter">

			<xsl:element name="numericValue"> <!-- stringValue OR dateTimeValue OR rangeTop/rangeBottom -->
				<xsl:value-of select="motorPosition" />
			</xsl:element>

			<xsl:element name="type">
				<xsl:element name="searchId">ParameterType [name = '<xsl:value-of select="concat('motor_', motorName)" />']</xsl:element> <!-- , '_', $slitType  -->
			</xsl:element>

			<xsl:element name="dataset">
				<id>-2</id>
			</xsl:element>

		</xsl:element>


	</xsl:template>

	<!-- ++++++++++++++++++++++++++++++++++++++++++++++++ -->
	<xsl:template match="date|disk|beamline|beamline|nameExp" /> <!-- In those cases do nothing! -->






</xsl:stylesheet>