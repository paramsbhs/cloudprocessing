import { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router";
import { Button } from "../components/ui/button";
import { Badge } from "../components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "../components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../components/ui/tabs";
import {
  Table, TableBody, TableCell, TableHead,
  TableHeader, TableRow,
} from "../components/ui/table";
import {
  LogOut, Upload, FileText, Download, Trash2,
  Zap, Loader2, Files, Briefcase, TrendingDown, Clock, AlertCircle, HardDrive,
} from "lucide-react";
import { UploadModal } from "../components/upload-modal";
import { JobDetailDrawer } from "../components/job-detail-drawer";
import { useAuth } from "../context/AuthContext";
import { api, FileRecord, JobRecord, StatsRecord, CompressionFormat } from "../api";

type FrontendStatus = "pending" | "uploaded" | "processing" | "completed" | "failed";

const statusMap: Record<string, FrontendStatus> = {
  PENDING_UPLOAD: "pending",
  UPLOADED:       "uploaded",
  PROCESSING:     "processing",
  COMPLETED:      "completed",
  FAILED:         "failed",
};

const statusColors: Record<FrontendStatus, string> = {
  pending:    "bg-gray-500/10 text-gray-400 border-gray-500/20",
  uploaded:   "bg-blue-500/10 text-blue-400 border-blue-500/20",
  processing: "bg-yellow-500/10 text-yellow-400 border-yellow-500/20",
  completed:  "bg-green-500/10 text-green-400 border-green-500/20",
  failed:     "bg-red-500/10 text-red-400 border-red-500/20",
};

const jobStatusColors: Record<string, string> = {
  QUEUED:     "bg-gray-500/10 text-gray-400 border-gray-500/20",
  PROCESSING: "bg-yellow-500/10 text-yellow-400 border-yellow-500/20",
  COMPLETED:  "bg-green-500/10 text-green-400 border-green-500/20",
  FAILED:     "bg-red-500/10 text-red-400 border-red-500/20",
};

function formatBytes(bytes: number | null): string {
  if (!bytes) return "—";
  const k = 1024;
  const sizes = ["B", "KB", "MB", "GB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${Math.round((bytes / Math.pow(k, i)) * 10) / 10} ${sizes[i]}`;
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("en-US", {
    month: "short", day: "numeric", year: "numeric",
  });
}

function formatMs(ms: number | null): string {
  if (!ms) return "—";
  if (ms < 1000) return `${ms}ms`;
  return `${(ms / 1000).toFixed(1)}s`;
}

function isImage(contentType: string): boolean {
  return contentType === "image/jpeg" || contentType === "image/png" || contentType === "image/webp" || contentType === "image/gif";
}

function isPdf(contentType: string): boolean {
  return contentType === "application/pdf";
}

export function Dashboard() {
  const navigate = useNavigate();
  const { email, logout, isAuthenticated } = useAuth();

  const [files, setFiles]             = useState<FileRecord[]>([]);
  const [jobs, setJobs]               = useState<JobRecord[]>([]);
  const [stats, setStats]             = useState<StatsRecord | null>(null);
  const [loading, setLoading]         = useState(true);
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [selectedFile, setSelectedFile] = useState<FileRecord | null>(null);
  const [compressingIds, setCompressingIds] = useState<Set<string>>(new Set());

  useEffect(() => {
    if (!isAuthenticated) navigate("/");
  }, [isAuthenticated, navigate]);

  const fetchAll = useCallback(async () => {
    try {
      const [filesData, jobsData, statsData] = await Promise.all([
        api.files.list(),
        api.jobs.list(),
        api.stats.get(),
      ]);
      setFiles(filesData);
      setJobs(jobsData);
      setStats(statsData);
    } catch {
      // token expiry handled by api client
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchAll(); }, [fetchAll]);

  // Poll every 3s while anything is processing
  useEffect(() => {
    const hasProcessing = files.some((f) => f.status === "PROCESSING")
      || jobs.some((j) => j.status === "PROCESSING" || j.status === "QUEUED");
    if (!hasProcessing) return;
    const id = setInterval(fetchAll, 3000);
    return () => clearInterval(id);
  }, [files, jobs, fetchAll]);

  // Keep drawer in sync after polling
  useEffect(() => {
    if (!selectedFile) return;
    const updated = files.find((f) => f.id === selectedFile.id);
    if (updated) setSelectedFile(updated);
  }, [files]);

  const handleLogout = () => { logout(); navigate("/"); };

  const handleUploadComplete = async (newFile: FileRecord) => {
    setIsUploadModalOpen(false);
    setFiles((prev) => [newFile, ...prev]);
    await fetchAll();
  };

  const handleDelete = async (_e: React.MouseEvent, fileId: string) => {
    try {
      await api.files.delete(fileId);
      setFiles((prev) => prev.filter((f) => f.id !== fileId));
      if (selectedFile?.id === fileId) setSelectedFile(null);
      await fetchAll();
    } catch (err: any) { alert(err.message); }
  };

  const handleCompress = async (fileId: string, format: CompressionFormat) => {
    setCompressingIds((prev) => new Set(prev).add(fileId));
    try {
      await api.jobs.create(fileId, format);
      await fetchAll();
    } catch (err: any) {
      alert(err.message);
    } finally {
      setCompressingIds((prev) => { const s = new Set(prev); s.delete(fileId); return s; });
    }
  };

  const handleDownload = async (_e: React.MouseEvent, fileId: string, original = false) => {
    try {
      const { downloadUrl } = await api.files.getDownloadUrl(fileId, original);
      window.open(downloadUrl, "_blank");
    } catch (err: any) { alert(err.message); }
  };

  return (
    <div className="min-h-screen">
      {/* Header */}
      <header className="border-b border-border bg-card">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <h1 className="text-xl font-semibold tracking-tight">CloudPress</h1>
          <div className="flex items-center gap-4">
            <span className="text-sm text-muted-foreground">{email}</span>
            <Button variant="ghost" size="sm" onClick={handleLogout}
              className="text-muted-foreground hover:text-foreground">
              <LogOut className="w-4 h-4 mr-2" />Logout
            </Button>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-6 py-8 space-y-8">

        {/* Stats cards */}
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
          <Card className="bg-card border-border">
            <CardHeader className="pb-1 pt-4 px-4">
              <CardTitle className="text-xs text-muted-foreground font-normal flex items-center gap-1.5">
                <Files className="w-3.5 h-3.5" />Total Files
              </CardTitle>
            </CardHeader>
            <CardContent className="px-4 pb-4">
              <p className="text-2xl font-semibold">{stats?.totalFiles ?? "—"}</p>
            </CardContent>
          </Card>

          <Card className="bg-card border-border">
            <CardHeader className="pb-1 pt-4 px-4">
              <CardTitle className="text-xs text-muted-foreground font-normal flex items-center gap-1.5">
                <Briefcase className="w-3.5 h-3.5" />Jobs Done
              </CardTitle>
            </CardHeader>
            <CardContent className="px-4 pb-4">
              <p className="text-2xl font-semibold text-green-400">{stats?.completedJobs ?? "—"}</p>
            </CardContent>
          </Card>

          <Card className="bg-card border-border">
            <CardHeader className="pb-1 pt-4 px-4">
              <CardTitle className="text-xs text-muted-foreground font-normal flex items-center gap-1.5">
                <AlertCircle className="w-3.5 h-3.5" />Failed
              </CardTitle>
            </CardHeader>
            <CardContent className="px-4 pb-4">
              <p className={`text-2xl font-semibold ${(stats?.failedJobs ?? 0) > 0 ? "text-red-400" : ""}`}>
                {stats?.failedJobs ?? "—"}
              </p>
            </CardContent>
          </Card>

          <Card className="bg-card border-border">
            <CardHeader className="pb-1 pt-4 px-4">
              <CardTitle className="text-xs text-muted-foreground font-normal flex items-center gap-1.5">
                <TrendingDown className="w-3.5 h-3.5" />Avg Compression
              </CardTitle>
            </CardHeader>
            <CardContent className="px-4 pb-4">
              <p className="text-2xl font-semibold text-green-400">
                {stats && stats.completedJobs > 0 ? `${stats.avgCompressionPercent.toFixed(1)}%` : "—"}
              </p>
            </CardContent>
          </Card>

          <Card className="bg-card border-border">
            <CardHeader className="pb-1 pt-4 px-4">
              <CardTitle className="text-xs text-muted-foreground font-normal flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5" />Avg Time
              </CardTitle>
            </CardHeader>
            <CardContent className="px-4 pb-4">
              <p className="text-2xl font-semibold">
                {stats && stats.completedJobs > 0 ? formatMs(stats.avgProcessingTimeMs) : "—"}
              </p>
            </CardContent>
          </Card>

          <Card className="bg-card border-border">
            <CardHeader className="pb-1 pt-4 px-4">
              <CardTitle className="text-xs text-muted-foreground font-normal flex items-center gap-1.5">
                <HardDrive className="w-3.5 h-3.5" />Space Saved
              </CardTitle>
            </CardHeader>
            <CardContent className="px-4 pb-4">
              <p className="text-2xl font-semibold text-green-400">
                {stats && stats.totalBytesSaved > 0 ? formatBytes(stats.totalBytesSaved) : "—"}
              </p>
            </CardContent>
          </Card>
        </div>

        {/* Tabs */}
        <Tabs defaultValue="files">
          <div className="flex items-center justify-between mb-4">
            <TabsList className="bg-secondary">
              <TabsTrigger value="files">Files</TabsTrigger>
              <TabsTrigger value="jobs">Jobs</TabsTrigger>
            </TabsList>
            <Button onClick={() => setIsUploadModalOpen(true)}
              className="bg-primary hover:bg-primary/90">
              <Upload className="w-4 h-4 mr-2" />Upload File
            </Button>
          </div>

          {/* Files tab */}
          <TabsContent value="files">
            {loading ? (
              <div className="text-center py-16 text-muted-foreground text-sm">Loading…</div>
            ) : files.length === 0 ? (
              <div className="border border-border rounded-lg bg-card p-12 text-center">
                <FileText className="w-12 h-12 mx-auto text-muted-foreground mb-4" />
                <h3 className="text-lg font-medium mb-2">No files yet</h3>
                <p className="text-sm text-muted-foreground mb-6">Upload your first file to get started</p>
                <Button onClick={() => setIsUploadModalOpen(true)} className="bg-primary hover:bg-primary/90">
                  <Upload className="w-4 h-4 mr-2" />Upload File
                </Button>
              </div>
            ) : (
              <div className="border border-border rounded-lg bg-card overflow-hidden">
                <Table>
                  <TableHeader>
                    <TableRow className="hover:bg-transparent border-border">
                      <TableHead className="text-muted-foreground">File Name</TableHead>
                      <TableHead className="text-muted-foreground">Size</TableHead>
                      <TableHead className="text-muted-foreground">Status</TableHead>
                      <TableHead className="text-muted-foreground">Date</TableHead>
                      <TableHead className="text-muted-foreground"></TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {files.map((file) => {
                      const status = statusMap[file.status] ?? "pending";
                      return (
                        <TableRow key={file.id}
                          className="border-border hover:bg-secondary/50 cursor-pointer"
                          onClick={() => setSelectedFile(file)}>
                          <TableCell className="font-medium">{file.originalName}</TableCell>
                          <TableCell className="text-muted-foreground">
                            {file.status === "COMPLETED" && file.outputSizeBytes ? (
                              <span className="flex items-center gap-1">
                                <span className="line-through">{formatBytes(file.sizeBytes)}</span>
                                <span className="text-green-400 font-medium">{formatBytes(file.outputSizeBytes)}</span>
                              </span>
                            ) : (
                              formatBytes(file.sizeBytes)
                            )}
                          </TableCell>
                          <TableCell>
                            <Badge variant="outline" className={`capitalize ${statusColors[status]}`}>
                              {status}
                            </Badge>
                          </TableCell>
                          <TableCell className="text-muted-foreground">{formatDate(file.createdAt)}</TableCell>
                          <TableCell onClick={(e) => e.stopPropagation()}>
                            <div className="flex items-center gap-1">
                              {file.status === "UPLOADED" && (
                                compressingIds.has(file.id) ? (
                                  <Button size="sm" variant="ghost" disabled
                                    className="text-primary opacity-50">
                                    <Loader2 className="w-4 h-4 mr-1 animate-spin" />Compressing…
                                  </Button>
                                ) : (
                                  <>
                                    {isImage(file.contentType) ? (
                                      <Button size="sm" variant="ghost"
                                        className="text-primary hover:text-primary/80 px-2"
                                        onClick={() => handleCompress(file.id, 'JPEG')}>
                                        <Zap className="w-3.5 h-3.5 mr-1" />.jpg
                                      </Button>
                                    ) : isPdf(file.contentType) ? (
                                      <Button size="sm" variant="ghost"
                                        className="text-primary hover:text-primary/80 px-2"
                                        onClick={() => handleCompress(file.id, 'GZIP')}>
                                        <Zap className="w-3.5 h-3.5 mr-1" />.pdf
                                      </Button>
                                    ) : (
                                      <>
                                        <Button size="sm" variant="ghost"
                                          className="text-primary hover:text-primary/80 px-2"
                                          onClick={() => handleCompress(file.id, 'GZIP')}>
                                          <Zap className="w-3.5 h-3.5 mr-1" />.gz
                                        </Button>
                                        <Button size="sm" variant="ghost"
                                          className="text-primary hover:text-primary/80 px-2"
                                          onClick={() => handleCompress(file.id, 'ZIP')}>
                                          .zip
                                        </Button>
                                      </>
                                    )}
                                  </>
                                )
                              )}
                              {file.status === "PROCESSING" && (
                                <span className="flex items-center gap-1 text-xs text-yellow-400 px-2">
                                  <Loader2 className="w-3 h-3 animate-spin" />Processing…
                                </span>
                              )}
                              {file.status === "COMPLETED" && (
                                <Button size="sm" variant="ghost"
                                  onClick={(e) => handleDownload(e, file.id)}>
                                  <Download className="w-4 h-4 mr-1" />Download
                                </Button>
                              )}
                              {(file.status === "UPLOADED" || file.status === "COMPLETED" || file.status === "FAILED") && (
                                <Button size="sm" variant="ghost"
                                  className="text-muted-foreground hover:text-foreground"
                                  onClick={(e) => handleDownload(e, file.id, true)}>
                                  <Download className="w-4 h-4 mr-1" />Original
                                </Button>
                              )}
                              <Button size="sm" variant="ghost"
                                className="text-muted-foreground hover:text-red-400"
                                onClick={(e) => handleDelete(e, file.id)}>
                                <Trash2 className="w-4 h-4" />
                              </Button>
                            </div>
                          </TableCell>
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              </div>
            )}
          </TabsContent>

          {/* Jobs tab */}
          <TabsContent value="jobs">
            {loading ? (
              <div className="text-center py-16 text-muted-foreground text-sm">Loading…</div>
            ) : jobs.length === 0 ? (
              <div className="border border-border rounded-lg bg-card p-12 text-center">
                <Briefcase className="w-12 h-12 mx-auto text-muted-foreground mb-4" />
                <h3 className="text-lg font-medium mb-2">No jobs yet</h3>
                <p className="text-sm text-muted-foreground">Compress a file to create your first job</p>
              </div>
            ) : (
              <div className="border border-border rounded-lg bg-card overflow-hidden">
                <Table>
                  <TableHeader>
                    <TableRow className="hover:bg-transparent border-border">
                      <TableHead className="text-muted-foreground">File</TableHead>
                      <TableHead className="text-muted-foreground">Status</TableHead>
                      <TableHead className="text-muted-foreground">Compression</TableHead>
                      <TableHead className="text-muted-foreground">Processing Time</TableHead>
                      <TableHead className="text-muted-foreground">Format</TableHead>
                      <TableHead className="text-muted-foreground">Attempts</TableHead>
                      <TableHead className="text-muted-foreground">Created</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {jobs.map((job) => (
                      <TableRow key={job.jobId} className="border-border hover:bg-secondary/50">
                        <TableCell className="font-medium max-w-[200px] truncate">
                          {job.originalName}
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center gap-2">
                            {(job.status === "PROCESSING" || job.status === "QUEUED") && (
                              <Loader2 className="w-3 h-3 animate-spin text-yellow-400" />
                            )}
                            <Badge variant="outline"
                              className={`capitalize ${jobStatusColors[job.status] ?? ""}`}>
                              {job.status.toLowerCase()}
                            </Badge>
                          </div>
                        </TableCell>
                        <TableCell className="text-muted-foreground">
                          {job.compressionRatio != null
                            ? <span className="text-green-400 font-medium">
                                {Math.round(job.compressionRatio * 100)}% smaller
                              </span>
                            : "—"}
                        </TableCell>
                        <TableCell className="text-muted-foreground">
                          {formatMs(job.processingTimeMs)}
                        </TableCell>
                        <TableCell className="text-muted-foreground text-xs">
                          {job.compressionFormat === 'GZIP' ? '.gz'
                            : job.compressionFormat === 'ZIP' ? '.zip'
                            : '.jpg'}
                        </TableCell>
                        <TableCell className="text-muted-foreground">
                          <span className={job.attemptCount > 1 ? "text-yellow-400" : ""}>
                            {job.attemptCount}/{job.maxAttempts}
                          </span>
                        </TableCell>
                        <TableCell className="text-muted-foreground">
                          {formatDate(job.createdAt)}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            )}
            {/* Failed job errors */}
            {jobs.some((j) => j.status === "FAILED" && j.errorMessage) && (
              <div className="mt-4 space-y-2">
                <p className="text-xs text-muted-foreground font-medium">Failure details</p>
                {jobs.filter((j) => j.status === "FAILED" && j.errorMessage).map((j) => (
                  <div key={j.jobId}
                    className="text-xs text-red-400 bg-red-500/5 border border-red-500/20 rounded px-3 py-2">
                    <span className="font-medium">{j.originalName}:</span> {j.errorMessage}
                  </div>
                ))}
              </div>
            )}
          </TabsContent>
        </Tabs>
      </main>

      <UploadModal
        open={isUploadModalOpen}
        onOpenChange={setIsUploadModalOpen}
        onUploadComplete={handleUploadComplete}
      />

      <JobDetailDrawer
        file={selectedFile}
        open={!!selectedFile}
        onOpenChange={(open) => !open && setSelectedFile(null)}
        onCompress={async (fileId, format) => {
          await api.jobs.create(fileId, format);
          await fetchAll();
        }}
      />
    </div>
  );
}
